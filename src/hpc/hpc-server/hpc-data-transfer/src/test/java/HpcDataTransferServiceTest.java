/**
 * HpcManagedDataService.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.service;

import gov.nih.nci.hpc.transfer.HpcDataTransfer;
import gov.nih.nci.hpc.transfer.impl.GlobusOnlineDataTranfer;

import java.util.List;

import org.globusonline.transfer.Authenticator;
import org.globusonline.transfer.JSONTransferAPIClient;

import gov.nih.nci.hpc.domain.dataset.HpcDataTransferLocations;
import gov.nih.nci.hpc.domain.dataset.HpcFileLocation;

public class HpcDataTransferServiceTest 
{         
    public static void main(String args[]) {

        try {
        	HpcDataTransferServiceTest ht = new HpcDataTransferServiceTest();
        	ht.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void run() throws Exception
    {
    	HpcDataTransfer dts = new GlobusOnlineDataTranfer();
    	HpcDataTransferLocations dtl = new HpcDataTransferLocations();

    	HpcFileLocation sourceLocation = new HpcFileLocation();
    	sourceLocation.setEndpoint("mahinarra#MNGOEP1");
    	sourceLocation.setPath("~/globusonline.txt");
    	dtl.setSource(sourceLocation);    	    	
    	
    	HpcFileLocation datasetLocation = new HpcFileLocation();
    	datasetLocation.setEndpoint("nihfnlcr#gridftp1");
    	datasetLocation.setPath("~/globusonline.txt");
    	dtl.setDestination(datasetLocation);
    	boolean transferDataset = dts.transferDataset(dtl,"","");
    }
}

 