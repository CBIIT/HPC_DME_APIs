/**
 * HpcDataTransferAccountValidatorProvider.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.integration;

import gov.nih.nci.hpc.domain.user.HpcIntegratedSystem;

/**
 * <p>
 * HPC Data Transfer Account Validator Provider Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$ 
 */

public interface HpcDataTransferAccountValidatorProvider 
{         
    /**
     * Get a data transfer account validator.
     *
     * @param accountType The data transfer type.
     * @return HpcDataTransferAccountValidatorProxy or null if no provider found.
     */
    public HpcDataTransferAccountValidatorProxy 
                                 get(HpcIntegratedSystem dataTransferSystem);
}

 