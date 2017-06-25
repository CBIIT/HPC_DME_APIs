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
	 * @namespace Inspectors.
	 *            <p>
	 *            Inspectors must implement an interface:
	 *            </p>
	 *            <tt>function( toInspect, type, names )</tt>
	 *            <p>
	 *            Each Inspector must look to the 'type' parameter and the
	 *            'names' array. These form a path into the domain object model.
	 *            For example the 'type' may be 'person' and the 'names' may be [
	 *            'address', 'street' ]. This would form a path into the domain
	 *            model of 'person/address/street' (i.e. return information on
	 *            the 'street' property within the 'address' property of the
	 *            'person' type).
	 *            </p>
	 */

	metawidget.inspector = metawidget.inspector || {};

	/**
	 * @class Delegates inspection to one or more sub-inspectors, then combines
	 *        the resulting metadata using
	 *        <tt>metawidget.util.combineInspectionResults</tt>.
	 *        <p>
	 *        The combining algorithm should be suitable for most use cases, but
	 *        one benefit of having a separate CompositeInspector is that
	 *        developers can replace it with their own version, with its own
	 *        combining algorithm, if required.
	 *        <p>
	 *        Note: the name <em>Composite</em>Inspector refers to the
	 *        Composite design pattern.
	 */

	metawidget.inspector.CompositeInspector = function( config ) {

		if ( ! ( this instanceof metawidget.inspector.CompositeInspector ) ) {
			throw new Error( 'Constructor called as a function' );
		}

		var _inspectors;

		if ( config.inspectors !== undefined ) {
			_inspectors = config.inspectors.slice( 0 );
		} else {
			_inspectors = config.slice( 0 );
		}

		this.inspect = function( toInspect, type, names ) {

			var compositeInspectionResult = {};

			for ( var ins = 0, insLength = _inspectors.length; ins < insLength; ins++ ) {

				var inspectionResult;
				var inspector = _inspectors[ins];

				if ( inspector.inspect !== undefined ) {
					inspectionResult = inspector.inspect( toInspect, type, names );
				} else {
					inspectionResult = inspector( toInspect, type, names );
				}

				metawidget.util.combineInspectionResults( compositeInspectionResult, inspectionResult );
			}

			return compositeInspectionResult;
		};
	};

	/**
	 * @class Inspects JavaScript objects for their property names and types.
	 *        <p>
	 *        In principal, ordering of property names within JavaScript objects
	 *        is not guaranteed. In practice, most browsers respect the original
	 *        order that properties were defined in. However you may want to
	 *        combine PropertyTypeInspector with a custom Inspector that imposes
	 *        a defined ordering using 'propertyOrder' attributes.
	 */

	metawidget.inspector.PropertyTypeInspector = function() {

		if ( ! ( this instanceof metawidget.inspector.PropertyTypeInspector ) ) {
			throw new Error( 'Constructor called as a function' );
		}
	};

	metawidget.inspector.PropertyTypeInspector.prototype.inspect = function( toInspect, type, names ) {

		/**
		 * Inspect the type of the property as best we can.
		 */

		function _getTypeOf( value ) {

			// JSON Schema primitive types are: 'array', 'boolean',
			// 'number', 'null', 'object' and 'string'

			if ( value instanceof Array ) {

				// typeof never returns 'array', even though JavaScript has
				// a built-in Array type

				return 'array';

			} else if ( value instanceof Date ) {

				// typeof never returns 'date', even though JavaScript has a
				// built-in Date type

				return 'date';

			} else {

				var typeOfProperty = typeof ( value );

				// type 'object' doesn't convey much, and can override a
				// more descriptive inspection result from a previous
				// Inspector. If you leave it off, Metawidget's default
				// behaviour is to recurse into the object anyway

				if ( typeOfProperty !== 'object' ) {
					return typeOfProperty;
				}
			}
		}

		// Traverse names

		toInspect = metawidget.util.traversePath( toInspect, names );

		var inspectionResult = {};

		// Inspect root node. Important if the Metawidget is
		// pointed directly at a primitive type

		if ( names !== undefined && names.length > 0 ) {
			inspectionResult.name = names[names.length - 1];
		} else {

			// Nothing useful to return?

			if ( toInspect === undefined ) {
				return;
			}
		}

		if ( toInspect !== undefined ) {

			inspectionResult.type = _getTypeOf( toInspect );

			if ( inspectionResult.type === undefined ) {

				inspectionResult.properties = {};

				for ( var property in toInspect ) {

					inspectionResult.properties[property] = {
						type: _getTypeOf( toInspect[property] )
					};
				}
			}
		}

		return inspectionResult;
	};

	/**
	 * @class Inspects JSON Schemas for their properties.
	 *        <p>
	 *        Because Metawidget <em>already</em> uses JSON Schema (v3)
	 *        internally as its inspection result format, this Inspector does
	 *        not need to do much. However it adds support for:
	 *        <p>
	 *        <ul>
	 *        <li>schemas that contain nested schemas (by traversing the given
	 *        'names' array)</li>
	 *        <li>checking the 'type' property of the schema</li>
	 *        <li>schemas that describe arrays (by traversing the 'items'
	 *        property)</li>
	 *        <li>schemas that have a top-level 'required' array (JSON Schema
	 *        v4)</li>
	 *        </ul>
	 */

	metawidget.inspector.JsonSchemaInspector = function( config ) {

		if ( ! ( this instanceof metawidget.inspector.JsonSchemaInspector ) ) {
			throw new Error( 'Constructor called as a function' );
		}

		var _schema;

		if ( config.schema !== undefined ) {
			_schema = config.schema;
		} else {
			_schema = config;
		}

		this.inspect = function( toInspect, type, names ) {

			/**
			 * Specialized version of <tt>metawidget.util.traversePath</tt>
			 * that supports 'properties' and 'items'.
			 */

			function _traversePath( toInspect, names ) {

				if ( toInspect === undefined ) {
					return undefined;
				}

				if ( names !== undefined ) {

					// Sanity check for passing a single string

					if ( ! ( names instanceof Array ) ) {
						throw new Error( "Expected array of names" );
					}

					for ( var loop = 0, length = names.length; loop < length; loop++ ) {

						// Support 'items' property (for arrays)

						var name = names[loop];

						if ( !isNaN( name ) ) {

							toInspect = toInspect.items;

							if ( toInspect === undefined ) {
								return undefined;
							}

							// We ignore the actual array index. We assume the
							// JSON Schema describes a homogeneous array,
							// regardless of the index

							continue;
						}

						// Support 'properties' property

						toInspect = toInspect.properties;

						if ( toInspect === undefined ) {
							return undefined;
						}

						toInspect = toInspect[name];

						// We don't need to worry about array indexes here: they
						// should have been parsed out by splitPath

						if ( toInspect === undefined ) {
							return undefined;
						}
					}
				}

				return toInspect;
			}

			// Traverse names using 'properties' and 'items' as appropriate

			var traversed = _traversePath( _schema, names );

			if ( traversed === undefined ) {
				return undefined;
			}

			// Copy values

			var inspectionResult = {};
			if ( names !== undefined ) {
				inspectionResult.name = names[names.length - 1];
			}
			metawidget.util.combineInspectionResults( inspectionResult, traversed );

			// Copy top-level 'required' array into each property (JSON Schema
			// v4)

			if ( inspectionResult.required !== undefined ) {

				for ( var loop = 0, length = inspectionResult.required.length; loop < length; loop++ ) {

					inspectionResult.properties[inspectionResult.required[loop]].required = true;
				}
			}

			return inspectionResult;
		};
	};
} )();