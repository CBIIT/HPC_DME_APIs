/**
 * HpcRequestContext.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import gov.nih.nci.hpc.domain.model.HpcRequestInvoker;

/**
 * HPC Request Context. Holds specific request (service call) data.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcRequestContext {
  //---------------------------------------------------------------------//
  // Instance members
  //---------------------------------------------------------------------//

  private static final ThreadLocal<HpcRequestInvoker> requestInvoker =
      new ThreadLocal<HpcRequestInvoker>() {
        @Override
        protected HpcRequestInvoker initialValue() {
          return new HpcRequestInvoker();
        }
      };

  /**
   * Get the invoker of this service-call.
   *
   * @return The HPC user who invoked this service.
   */
  public static HpcRequestInvoker getRequestInvoker() {
    return requestInvoker.get();
  }

  /**
   * Set the invoker who invoked this service-call.
   *
   * @param invoker The request invoker.
   */
  public static void setRequestInvoker(HpcRequestInvoker invoker) {
    requestInvoker.set(invoker);
  }
}
