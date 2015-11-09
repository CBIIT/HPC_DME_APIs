/**
 * HpcDataTransferAccountValidatorProxy.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration;

import gov.nih.nci.hpc.domain.user.HpcIntegratedSystemAccount;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Data Transfer Account Validator Proxy Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$ 
 */

public interface HpcDataTransferAccountValidatorProxy 
{         
    /**
     * Validate a data transfer account.
     *
     * @param dataTransferAccount The account to use for the transfer.
     * @return True if the account is valid, or false otherwise.
     * 
     * @throws HpcException
     */
    public boolean validateDataTransferAccount(
    		                   HpcIntegratedSystemAccount dataTransferAccount)
    		                   throws HpcException; 
}

 