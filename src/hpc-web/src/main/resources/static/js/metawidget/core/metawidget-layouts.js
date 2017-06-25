// Metawidget 4.1
//
// This file is dual licensed under both the LGPL
// (http://www.gnu.org/licenses/lgpl-2.1.html) and the EPL
// (http://www.eclipse.org/org/documents/epl-v10.php). As a
// recipient of Metawidget, you may choose to receive it under either
// the LGPL or the EPL.
//
// Commercial licenses are also available. See http://metawidget.org
// for details.

/**
 * @author <a href="http://kennardconsulting.com">Richard Kennard</a>
 */

var metawidget = metawidget || {};

( function() {

	'use strict';

	/**
	 * @namespace Layouts.
	 */

	metawidget.layout = metawidget.layout || {};

	/**
	 * @class Layout to simply output components one after another, with no
	 *        labels and no structure. This Layout is suited to rendering single
	 *        components, or for rendering components whose layout relies
	 *        entirely on CSS.
	 */

	metawidget.layout.SimpleLayout = function() {

		if ( ! ( this instanceof metawidget.layout.SimpleLayout ) ) {
			throw new Error( 'Constructor called as a function' );
		}
	};

	metawidget.layout.SimpleLayout.prototype.layoutWidget = function( widget, elementName, attributes, container ) {

		if ( widget.tagName === 'STUB' && !metawidget.util.hasChildElements( widget ) ) {
			return;
		}

		container.appendChild( widget );
	};

	/**
	 * @class Layout to arrange widgets using dl/dt/dd tags.
	 */

	metawidget.layout.DefinitionListLayout = function( config ) {

		if ( ! ( this instanceof metawidget.layout.DefinitionListLayout ) ) {
			throw new Error( 'Constructor called as a function' );
		}

		var _labelStyleClass = config !== undefined ? config.labelStyleClass : undefined;
		var _labelSuffix = config !== undefined && config.labelSuffix !== undefined ? config.labelSuffix : ':';

		this.startContainerLayout = function( container, mw ) {

			var dl = metawidget.util.createElement( mw, 'dl' );
			if ( mw.path !== undefined ) {
				var id = metawidget.util.getId( "property", {}, mw );
				if ( id !== undefined ) {
					dl.setAttribute( 'id', 'dl-' + id );
				}
			}

			container.appendChild( dl );
		};

		this.layoutWidget = function( widget, elementName, attributes, container, mw ) {

			if ( widget.tagName === 'STUB' && !metawidget.util.hasChildElements( widget ) ) {
				return;
			}

			// Label

			var dl = container.childNodes[container.childNodes.length - 1];
			this.layoutLabel( dl, widget, elementName, attributes, mw );

			// Widget

			var dd = metawidget.util.createElement( mw, 'dd' );
			dd.appendChild( widget );
			dl.appendChild( dd );
		};

		this.layoutLabel = function( dl, widget, elementName, attributes, mw ) {

			if ( elementName === 'entity' || elementName === 'action' ) {
				return;
			}

			if ( attributes.name === undefined && attributes.title === undefined ) {
				return;
			}

			var labelString = metawidget.util.getLabelString( attributes, mw );

			if ( labelString === '' || labelString === null ) {
				return;
			}

			var dt = metawidget.util.createElement( mw, 'dt' );

			var label = metawidget.util.createElement( mw, 'label' );
			if ( widget.getAttribute( 'id' ) !== null ) {
				label.setAttribute( 'for', widget.getAttribute( 'id' ) );
			}

			if ( _labelStyleClass !== undefined ) {
				label.setAttribute( 'class', _labelStyleClass );
			}

			label.innerHTML = labelString + _labelSuffix;

			dt.appendChild( label );
			dl.appendChild( dt );
		};
	};

	/**
	 * @class Layout to arrange widgets using div tags.
	 */

	metawidget.layout.DivLayout = function( config ) {

		if ( ! ( this instanceof metawidget.layout.DivLayout ) ) {
			throw new Error( 'Constructor called as a function' );
		}

		var _divStyleClasses = config !== undefined ? config.divStyleClasses : undefined;
		var _labelStyleClass = config !== undefined ? config.labelStyleClass : undefined;
		var _labelSuffix = config !== undefined && config.labelSuffix !== undefined ? config.labelSuffix : ':';
		var _suppressDivAroundLabel = config !== undefined && config.suppressDivAroundLabel !== undefined ? config.suppressDivAroundLabel : false;
		var _suppressDivAroundWidget = config !== undefined && config.suppressDivAroundWidget !== undefined ? config.suppressDivAroundWidget : false;
		var _appendRequiredClassOnLabelDiv = config !== undefined && config.appendRequiredClassOnLabelDiv !== undefined ? config.appendRequiredClassOnLabelDiv : undefined;
		var _appendRequiredClassOnWidgetDiv = config !== undefined && config.appendRequiredClassOnWidgetDiv !== undefined ? config.appendRequiredClassOnWidgetDiv : undefined;

		// REFACTOR: make this _suppressLabelSuffixOn and allow pass array of
		// types

		var _suppressLabelSuffixOnCheckboxes = config !== undefined && config.suppressLabelSuffixOnCheckboxes !== undefined ? config.suppressLabelSuffixOnCheckboxes : false;
		var _wrapInsideLabels = config !== undefined && config.wrapInsideLabels !== undefined ? config.wrapInsideLabels : undefined;
		var _wrapWithExtraDiv = config !== undefined && config.wrapWithExtraDiv !== undefined ? config.wrapWithExtraDiv : undefined;

		this.layoutWidget = function( widget, elementName, attributes, container, mw ) {

			if ( widget.tagName === 'STUB' && !metawidget.util.hasChildElements( widget ) ) {
				return;
			}

			// Collapse buttons into the previous div, if it also contained a
			// button

			if ( widget.tagName === 'INPUT' && ( widget.getAttribute( 'type' ) === 'button' || widget.getAttribute( 'type' ) === 'submit' ) ) {

				if ( container.childNodes.length > 0 ) {

					var lastOuterDiv = container.childNodes[container.childNodes.length - 1];
					if ( lastOuterDiv.childNodes.length === 1 ) {
						var lastWidgetDiv = lastOuterDiv.childNodes[0];
						if ( lastWidgetDiv.childNodes.length > 0 ) {
							var lastWidget = lastWidgetDiv.childNodes[lastWidgetDiv.childNodes.length - 1];
							if ( lastWidget.tagName === 'INPUT' && ( lastWidget.getAttribute( 'type' ) === 'button' || lastWidget.getAttribute( 'type' ) === 'submit' ) ) {
								lastWidgetDiv.appendChild( widget );
								return;
							}
						}
					}
				}
			}

			var outerDiv = metawidget.util.createElement( mw, 'div' );
			if ( _divStyleClasses !== undefined && _divStyleClasses[0] !== undefined ) {
				outerDiv.setAttribute( 'class', _divStyleClasses[0] );
			}

			// Label

			var labelWidget = this.layoutLabel( outerDiv, widget, elementName, attributes, mw );

			// Widget
			
			var toAppendToOuterDiv = widget;

			// _wrapInsideLabels

			if ( widget.tagName === 'INPUT' && metawidget.util.niceIndexOf( _wrapInsideLabels, widget.getAttribute( 'type' ) ) !== -1 ) {
				labelWidget.insertBefore( widget, labelWidget.firstChild );
				toAppendToOuterDiv = labelWidget;
			} else {
				toAppendToOuterDiv = widget;
			}

			// _wrapWithExtraDiv

			if ( widget.tagName === 'INPUT' && _wrapWithExtraDiv !== undefined && _wrapWithExtraDiv[widget.getAttribute( 'type' )] !== undefined ) {

				var extraDiv = metawidget.util.createElement( mw, 'div' );
				extraDiv.setAttribute( 'class', _wrapWithExtraDiv[widget.getAttribute( 'type' )] );
				extraDiv.appendChild( toAppendToOuterDiv );
				toAppendToOuterDiv = extraDiv;
			}

			// Wrap with div
			
			if ( _suppressDivAroundWidget !== true ) {
				var widgetDiv = metawidget.util.createElement( mw, 'div' );
				if ( _divStyleClasses !== undefined && _divStyleClasses[2] !== undefined ) {
					widgetDiv.setAttribute( 'class', _divStyleClasses[2] );
				}

				// Useful for CSS :after selectors

				if ( metawidget.util.isTrueOrTrueString( attributes.required ) && _appendRequiredClassOnWidgetDiv !== undefined ) {
					metawidget.util.appendToAttribute( widgetDiv, 'class', _appendRequiredClassOnWidgetDiv );
				}
				widgetDiv.appendChild( toAppendToOuterDiv );
				toAppendToOuterDiv = widgetDiv;
			}

			outerDiv.appendChild( toAppendToOuterDiv );
			container.appendChild( outerDiv );
		};

		/**
		 * @return the label widget
		 */

		this.layoutLabel = function( outerDiv, widget, elementName, attributes, mw ) {

			if ( elementName === 'entity' || elementName === 'action' ) {
				return;
			}

			if ( attributes.name === undefined && attributes.title === undefined ) {
				return;
			}

			var labelString = this.getLabelString( widget, attributes, mw );

			if ( labelString === '' || labelString === null ) {
				return;
			}

			var label = metawidget.util.createElement( mw, 'label' );
			if ( widget.getAttribute( 'id' ) !== null ) {
				label.setAttribute( 'for', widget.getAttribute( 'id' ) );
				label.setAttribute( 'id', widget.getAttribute( 'id' ) + '-label' );
			}

			if ( _labelStyleClass !== undefined ) {
				label.setAttribute( 'class', _labelStyleClass );
			}

			label.innerHTML = labelString;

			if ( _suppressDivAroundLabel === true ) {
				outerDiv.appendChild( label );
			} else {
				var labelDiv = metawidget.util.createElement( mw, 'div' );
				if ( _divStyleClasses !== undefined && _divStyleClasses[1] !== undefined ) {
					labelDiv.setAttribute( 'class', _divStyleClasses[1] );
				}

				// Useful for CSS :after selectors

				if ( metawidget.util.isTrueOrTrueString( attributes.required ) && _appendRequiredClassOnLabelDiv !== undefined ) {
					metawidget.util.appendToAttribute( labelDiv, 'class', _appendRequiredClassOnLabelDiv );
				}

				labelDiv.appendChild( label );
				outerDiv.appendChild( labelDiv );
			}

			return label;
		};

		/**
		 * @returns the label string, or a blank string if no label.
		 */

		this.getLabelString = function( widget, attributes, mw ) {

			var labelString = metawidget.util.getLabelString( attributes, mw );

			if ( labelString === '' || labelString === null ) {
				return labelString;
			}

			// Some UI frameworks (like JQuery Mobile) reuse the checkbox label
			// alongside the checkbox itself. This looks bad if we keep the
			// suffix

			if ( _suppressLabelSuffixOnCheckboxes === true && widget.tagName === 'INPUT' ) {
				if ( widget.getAttribute( 'type' ) === 'checkbox' || widget.getAttribute( 'type' ) === 'radio' ) {
					return labelString;
				}
			}

			return labelString + _labelSuffix;
		};
	};

	/**
	 * @class Layout to arrange widgets in a table, with one column for the
	 *        label and another for the widget.
	 */

	metawidget.layout.TableLayout = function( config ) {

		if ( ! ( this instanceof metawidget.layout.TableLayout ) ) {
			throw new Error( 'Constructor called as a function' );
		}

		var _tableStyleClass = config !== undefined ? config.tableStyleClass : undefined;
		var _columnStyleClasses = config !== undefined ? config.columnStyleClasses : undefined;
		var _headerStyleClass = config !== undefined ? config.headerStyleClass : undefined;
		var _footerStyleClass = config !== undefined ? config.footerStyleClass : undefined;
		var _numberOfColumns = config !== undefined && config.numberOfColumns ? config.numberOfColumns : 1;
		var _currentColumn = 0;

		this.startContainerLayout = function( container, mw ) {

			var table = metawidget.util.createElement( mw, 'table' );
			if ( mw.path !== undefined ) {
				var id = metawidget.util.getId( "property", {}, mw );
				if ( id !== undefined ) {
					table.setAttribute( 'id', 'table-' + id );
				}
			}

			if ( _tableStyleClass !== undefined ) {
				table.setAttribute( 'class', _tableStyleClass );
			}

			container.appendChild( table );

			// Facets

			if ( mw.overriddenNodes !== undefined ) {
				for ( var loop1 = 0, length1 = mw.overriddenNodes.length; loop1 < length1; loop1++ ) {

					var child = mw.overriddenNodes[loop1];

					if ( child.tagName !== 'FACET' ) {
						continue;
					}

					// thead or tfoot

					var parent;

					if ( child.getAttribute( 'name' ) === 'header' ) {
						parent = metawidget.util.createElement( mw, 'thead' );
					} else if ( child.getAttribute( 'name' ) === 'footer' ) {
						parent = metawidget.util.createElement( mw, 'tfoot' );
					} else {
						continue;
					}

					table.appendChild( parent );
					var tr = metawidget.util.createElement( mw, 'tr' );
					parent.appendChild( tr );
					var td = metawidget.util.createElement( mw, 'td' );
					td.setAttribute( 'colspan', _numberOfColumns * 3 );

					if ( child.getAttribute( 'name' ) === 'header' ) {
						if ( _headerStyleClass !== undefined ) {
							td.setAttribute( 'class', _headerStyleClass );
						}
					} else {
						if ( _footerStyleClass !== undefined ) {
							td.setAttribute( 'class', _footerStyleClass );
						}
					}

					tr.appendChild( td );

					// Append children, so as to unwrap the 'facet' tag

					while ( child.childNodes.length > 0 ) {
						td.appendChild( child.removeChild( child.childNodes[0] ) );
					}
				}
			}

			// tbody

			table.appendChild( metawidget.util.createElement( mw, 'tbody' ) );
		};

		this.layoutWidget = function( widget, elementName, attributes, container, mw ) {

			// Do not render empty stubs

			if ( widget.tagName === 'STUB' && !metawidget.util.hasChildElements( widget ) ) {
				return;
			}

			// Special support for large components

			var spanAllColumns = metawidget.util.isSpanAllColumns( attributes );

			if ( spanAllColumns === true && _currentColumn > 0 ) {
				_currentColumn = 0;
			}

			// Id

			var table = container.childNodes[container.childNodes.length - 1];
			var idPrefix = undefined;

			if ( attributes.name !== undefined ) {
				if ( metawidget.util.hasAttribute( table, 'id' )) {
					idPrefix = table.getAttribute( 'id' );
				}

				if ( idPrefix !== undefined ) {
					if ( elementName !== 'entity' ) {
						if ( idPrefix.charAt( idPrefix.length - 1 ) !== '-' ) {
							idPrefix += metawidget.util.capitalize( attributes.name );
						} else {
							idPrefix += attributes.name;
						}
					}
				} else {
					idPrefix = 'table-' + attributes.name;
				}
			}

			// Start column

			var tbody = table.childNodes[table.childNodes.length - 1];
			var tr;

			if ( _currentColumn === 0 ) {
				tr = metawidget.util.createElement( mw, 'tr' );
				if ( idPrefix !== undefined ) {
					tr.setAttribute( 'id', idPrefix + '-row' );
				}
				tbody.appendChild( tr );
			} else {
				tr = tbody.childNodes[tbody.childNodes.length - 1];
			}

			// Label

			this.layoutLabel( tr, idPrefix, widget, elementName, attributes, mw );

			// Widget

			var td = metawidget.util.createElement( mw, 'td' );

			if ( idPrefix !== undefined ) {
				td.setAttribute( 'id', idPrefix + '-cell' );
			}

			if ( _columnStyleClasses !== undefined && _columnStyleClasses[1] !== undefined ) {
				td.setAttribute( 'class', _columnStyleClasses[1] );
			}

			if ( spanAllColumns === true ) {
				td.setAttribute( 'colspan', ( ( _numberOfColumns * 3 ) - 1 ) - tr.childNodes.length );
			} else if ( tr.childNodes.length < 1 ) {
				td.setAttribute( 'colspan', 2 - tr.childNodes.length );
			}

			td.appendChild( widget );
			tr.appendChild( td );

			// Required

			this.layoutRequired( tr, attributes, mw );

			// Next column

			if ( spanAllColumns === true ) {
				_currentColumn = _numberOfColumns - 1;
			}

			_currentColumn = ( _currentColumn + 1 ) % _numberOfColumns;
		};

		this.layoutLabel = function( tr, idPrefix, widget, elementName, attributes, mw ) {

			if ( elementName === 'entity' ) {
				return;
			}

			if ( attributes.name === undefined && attributes.title === undefined ) {
				return;
			}

			var labelString = this.getLabelString( attributes, mw );

			if ( labelString === null ) {
				return;
			}

			// Label

			var th = metawidget.util.createElement( mw, 'th' );

			if ( idPrefix !== undefined ) {
				th.setAttribute( 'id', idPrefix + '-label-cell' );
			}

			if ( _columnStyleClasses !== undefined && _columnStyleClasses[0] !== undefined ) {
				th.setAttribute( 'class', _columnStyleClasses[0] );
			}

			if ( elementName !== 'action' && labelString !== '' ) {
				var label = metawidget.util.createElement( mw, 'label' );

				if ( metawidget.util.hasAttribute( widget, 'id' )) {
					label.setAttribute( 'for', widget.getAttribute( 'id' ) );
				}

				if ( idPrefix !== undefined ) {
					label.setAttribute( 'id', idPrefix + '-label' );
				}

				label.innerHTML = labelString;
				th.appendChild( label );
			}

			tr.appendChild( th );
		};

		this.layoutRequired = function( tr, attributes, mw ) {

			var td = metawidget.util.createElement( mw, 'td' );

			if ( _columnStyleClasses !== undefined && _columnStyleClasses[2] !== undefined ) {
				td.setAttribute( 'class', _columnStyleClasses[2] );
			}

			if ( !metawidget.util.isTrueOrTrueString( attributes.readOnly ) && metawidget.util.isTrueOrTrueString( attributes.required ) ) {
				td.innerHTML = '*';
			}

			tr.appendChild( td );
		};

		/**
		 * @returns the label string, a blank string if no label, or null
		 */

		this.getLabelString = function( attributes, mw ) {

			var labelString = metawidget.util.getLabelString( attributes, mw );

			if ( labelString === '' || labelString === null ) {
				return labelString;
			}

			return labelString + ':';
		};
	};

	//
	// LayoutDecorator
	//

	/**
	 * Augment the given 'decorator' with methods suitable for making section
	 * separator LayoutDecorators.
	 * <p>
	 * This includes implementing <tt>onStartBuild</tt>,
	 * <tt>startContainerLayout</tt>, <tt>endContainerLayout</tt> and
	 * <tt>onEndBuild</tt> methods.
	 */

	metawidget.layout._createSectionLayoutDecorator = function( config, decorator, decoratorName ) {

		var _delegate;

		if ( config.delegate !== undefined ) {
			_delegate = config.delegate;
		} else {
			_delegate = config;
		}

		/**
		 * Read-only getter.
		 * <p>
		 * Dangerous to add a public 'delegate' property, because can conflict
		 * with 'config.delegate'.
		 */

		decorator.getDelegate = function() {

			return _delegate;
		};

		decorator.onStartBuild = function( mw ) {

			if ( decorator.getDelegate().onStartBuild !== undefined ) {
				decorator.getDelegate().onStartBuild( mw );
			}
		};

		decorator.startContainerLayout = function( container, mw ) {

			container[decoratorName] = {};

			if ( decorator.getDelegate().startContainerLayout !== undefined ) {
				decorator.getDelegate().startContainerLayout( container, mw );
			}
		};

		decorator.endContainerLayout = function( container, mw ) {

			if ( decorator.getDelegate().endContainerLayout !== undefined ) {
				decorator.getDelegate().endContainerLayout( container, mw );
			}

			container[decoratorName] = {};
		};

		decorator.onEndBuild = function( mw ) {

			if ( decorator.getDelegate().onEndBuild !== undefined ) {
				decorator.getDelegate().onEndBuild( mw );
			}
		};
	};

	/**
	 * Augment the given 'decorator' with methods suitable for making flat (as
	 * opposed to nested) section separator LayoutDecorators.
	 * <p>
	 * This includes an implementation of the <tt>layoutWidget</tt> method and
	 * a declaration of a <tt>addSectionWidget</tt> method.
	 */

	metawidget.layout.createFlatSectionLayoutDecorator = function( config, decorator, decoratorName ) {

		if ( this instanceof metawidget.layout.createFlatSectionLayoutDecorator ) {
			throw new Error( 'Function called as a Constructor' );
		}

		metawidget.layout._createSectionLayoutDecorator( config, decorator, decoratorName );

		decorator.layoutWidget = function( widget, elementName, attributes, container, mw ) {

			var section;

			// If our delegate is itself a NestedSectionLayoutDecorator, strip
			// the section

			if ( decorator.getDelegate().nestedSectionLayoutDecorator === true ) {

				// Stay where we are?

				section = metawidget.util.stripSection( attributes );

				if ( section === undefined || section === container[decoratorName].currentSection ) {
					return decorator.getDelegate().layoutWidget( widget, elementName, attributes, container, mw );
				}

				// End nested LayoutDecorator's current section

				if ( container[decoratorName].currentSection !== undefined ) {
					decorator.getDelegate().endContainerLayout( container, mw );
				}

				container[decoratorName].currentSection = section;

				// Add a heading

				if ( section !== '' ) {
					decorator.addSectionWidget( section, 0, attributes, container, mw );
				}
			} else {

				// Stay where we are?

				if ( attributes.section === undefined || attributes.section === container[decoratorName].currentSection ) {
					return decorator.getDelegate().layoutWidget( widget, elementName, attributes, container, mw );
				}

				// For each of the new sections...

				var sections = attributes.section;

				if ( ! ( sections instanceof Array ) ) {
					sections = [ sections ];
				}

				var currentSections;

				if ( container[decoratorName].currentSection !== undefined ) {
					currentSections = container[decoratorName].currentSection;
				} else {
					currentSections = [];
				}

				for ( var level = 0; level < sections.length; level++ ) {
					section = sections[level];

					// ...that are different from our current...

					if ( section === '' ) {
						continue;
					}

					if ( level < currentSections.length && section === currentSections[level] ) {
						continue;
					}

					// ...add a heading
					//
					// Note: we cannot stop/start the delegate layout here. It
					// is tempting, but remember addSectionWidget needs to use
					// the delegate. If you stop/add section heading/start the
					// delegate, who is laying out the section heading?

					decorator.addSectionWidget( section, level, attributes, container, mw );
				}

				container[decoratorName].currentSection = sections;
			}

			// Add component as normal

			decorator.getDelegate().layoutWidget( widget, elementName, attributes, container, mw );
		};
	};

	/**
	 * Augment the given 'decorator' with methods suitable for making nested (as
	 * opposed to flat) section separator LayoutDecorators.
	 * <p>
	 * This includes an implementation of the <tt>layoutWidget</tt> method and
	 * a declaration of a <tt>createSectionWidget</tt> method.
	 */

	metawidget.layout.createNestedSectionLayoutDecorator = function( config, decorator, decoratorName ) {

		if ( this instanceof metawidget.layout.createNestedSectionLayoutDecorator ) {
			throw new Error( 'Function called as a Constructor' );
		}

		metawidget.layout._createSectionLayoutDecorator( config, decorator, decoratorName );

		// Tag this NestedSectionLayoutDecorator so that
		// FlatSectionLayoutDecorator can recognize it

		decorator.nestedSectionLayoutDecorator = true;

		decorator.layoutWidget = function( widget, elementName, attributes, container, mw ) {

			// Stay where we are?

			var section = metawidget.util.stripSection( attributes );

			if ( section === undefined || section === container[decoratorName].currentSection ) {
				if ( container[decoratorName].currentSectionWidget ) {
					return decorator.getDelegate().layoutWidget( widget, elementName, attributes, container[decoratorName].currentSectionWidget, mw );
				}
				return decorator.getDelegate().layoutWidget( widget, elementName, attributes, container, mw );
			}

			// End current section

			if ( container[decoratorName].currentSectionWidget !== undefined ) {
				decorator.endContainerLayout( container[decoratorName].currentSectionWidget, mw );
			}

			container[decoratorName].currentSection = section;
			var previousSectionWidget = container[decoratorName].currentSectionWidget;
			delete container[decoratorName].currentSectionWidget;

			// No new section?

			if ( section === '' ) {
				decorator.getDelegate().layoutWidget( widget, elementName, attributes, container, mw );
				return;
			}

			// Start new section

			container[decoratorName].currentSectionWidget = decorator.createSectionWidget( previousSectionWidget, section, attributes, container, mw );
			decorator.startContainerLayout( container[decoratorName].currentSectionWidget, mw );

			// Add component to new section

			decorator.getDelegate().layoutWidget( widget, elementName, attributes, container[decoratorName].currentSectionWidget, mw );
		};

		var _superEndContainerLayout = decorator.endContainerLayout;

		decorator.endContainerLayout = function( container, mw ) {

			// End hanging layouts

			if ( container[decoratorName].currentSectionWidget !== undefined ) {
				decorator.endContainerLayout( container[decoratorName].currentSectionWidget, mw );
			}

			_superEndContainerLayout.call( this, container, mw );
		};
	};

	/**
	 * @class LayoutDecorator to decorate widgets from different sections using
	 *        an HTML heading tag (i.e. <tt>h1</tt>, <tt>h2</tt> etc).
	 */

	metawidget.layout.HeadingTagLayoutDecorator = function( config ) {

		if ( ! ( this instanceof metawidget.layout.HeadingTagLayoutDecorator ) ) {
			throw new Error( 'Constructor called as a function' );
		}

		metawidget.layout.createFlatSectionLayoutDecorator( config, this, 'headingTagLayoutDecorator' );
	};

	metawidget.layout.HeadingTagLayoutDecorator.prototype.addSectionWidget = function( section, level, attributes, container, mw ) {

		var h1 = metawidget.util.createElement( mw, 'h' + ( level + 1 ) );
		h1.innerHTML = section;

		this.getDelegate().layoutWidget( h1, "property", {
			wide: 'true'
		}, container, mw );
	};

	/**
	 * @class LayoutDecorator to decorate widgets from different sections using
	 *        nested <tt>div</tt> tags.
	 */

	metawidget.layout.DivLayoutDecorator = function( config ) {

		if ( ! ( this instanceof metawidget.layout.DivLayoutDecorator ) ) {
			throw new Error( 'Constructor called as a function' );
		}

		metawidget.layout.createNestedSectionLayoutDecorator( config, this, 'divLayoutDecorator' );
	};

	metawidget.layout.DivLayoutDecorator.prototype.createSectionWidget = function( previousSectionWidget, section, attributes, container, mw ) {

		var div = metawidget.util.createElement( mw, 'div' );
		div.setAttribute( 'title', section );
		this.getDelegate().layoutWidget( div, "property", {
			wide: 'true'
		}, container, mw );

		return div;
	};
} )();