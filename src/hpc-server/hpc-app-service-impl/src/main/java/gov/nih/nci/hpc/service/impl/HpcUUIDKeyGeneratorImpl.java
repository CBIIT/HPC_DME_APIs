/**
 * HpcUUIDKeyGeneratorImpl.java
 *
 * <p>Copyright SVG, Inc. Copyright Leidos Biomedical Research, Inc
 *
 * <p>Distributed under the OSI-approved BSD 3-Clause License. See
 * http://ncip.github.com/HPC/LICENSE.txt for details.
 */
package gov.nih.nci.hpc.service.impl;

import java.util.UUID;

/**
 * HPC Key generator w/ UUID.
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 */
public class HpcUUIDKeyGeneratorImpl implements HpcKeyGenerator {
  //---------------------------------------------------------------------//
  // HpcKeyGenerator Interface Implementation
  //---------------------------------------------------------------------//

  @Override
  public String generateKey() {
    return UUID.randomUUID().toString();
  }

  @Override
  public boolean validateKey(String key) {
    try {
      if (key == null || UUID.fromString(key) == null) {
        return false;
      }

    } catch (IllegalArgumentException e) {
      return false;
    }

    return true;
  }
}
