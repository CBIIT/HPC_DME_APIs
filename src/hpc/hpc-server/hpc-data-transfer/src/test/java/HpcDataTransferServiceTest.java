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

import gov.nih.nci.hpc.domain.HpcDataset;
import gov.nih.nci.hpc.domain.HpcDatasetLocation;
import gov.nih.nci.hpc.domain.HpcFacility;
import gov.nih.nci.hpc.domain.HpcDatasetType;

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
    	HpcDataset dataset = new HpcDataset();
    	dataset.setId("1");
    	dataset.setName("TEST");
    	dataset.setType(HpcDatasetType.BAM);
    	dataset.setSize(100);

    	HpcDatasetLocation sourceLocation = new HpcDatasetLocation();
    	sourceLocation.setFacility(HpcFacility.UNKONWN);
    	sourceLocation.setEndpoint("mahinarra#MNGOEP1");
    	sourceLocation.setFilePath("~/globusonline.txt");
    	sourceLocation.setDataTransfer(gov.nih.nci.hpc.domain.HpcDataTransfer.GLOBUS);
    	dataset.setSource(sourceLocation);    	    	
    	
    	HpcDatasetLocation datasetLocation = new HpcDatasetLocation();
    	datasetLocation.setFacility(HpcFacility.FREDERICK);
    	datasetLocation.setEndpoint("nihfnlcr#gridftp1");
    	datasetLocation.setFilePath("~/globusonline.txt");
    	datasetLocation.setDataTransfer(gov.nih.nci.hpc.domain.HpcDataTransfer.GLOBUS);
    	dataset.setLocation(datasetLocation);
    	boolean transferDataset = dts.transferDataset(dataset);
    }
}

 