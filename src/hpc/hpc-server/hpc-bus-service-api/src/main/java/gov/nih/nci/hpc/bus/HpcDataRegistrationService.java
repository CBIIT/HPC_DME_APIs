/**
 * HpcDataRegistrationService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.bus;

import gov.nih.nci.hpc.dto.HpcDataRegistrationInput;
import gov.nih.nci.hpc.exception.HpcException;

/**
 * <p>
 * HPC Datasets Registration Business Service Interface.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public interface HpcDataRegistrationService 
{         
    /**
     * Register Datasets.
     *
     * @param registrationInput The datas registration input DTO.
     * @return The registered data ID.
     * 
     * @throws HpcException
     */
    public String registerData(
    		              HpcDataRegistrationInput registrationInput)
    		              throws HpcException;
}

 