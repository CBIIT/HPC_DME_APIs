package gov.nih.nci.hpc.web.util;

import gov.nih.nci.hpc.dto.security.HpcUserDTO;
import javax.servlet.http.HttpSession;

/**
 * Utility class for methods supporting User identity related logic.
 *
 * @author liuwy
 */
public class HpcIdentityUtil {

    /**
     * Check if current authenticated User has the Role of Group Admin.
     *
     * @param session  The HTTP session
     * @return true if User has Group Admin Role, false otherwise
     */
    public static boolean isUserGroupAdmin(HttpSession session) {
        boolean retVal = false;
        if (null != session &&
            session.getAttribute("hpcUser") instanceof HpcUserDTO)
        {
            HpcUserDTO userDto = (HpcUserDTO) session.getAttribute("hpcUser");
            //TODO query against data_curator metadata
            retVal = "GROUP_ADMIN".equals(userDto.getUserRole());
        }
        return retVal;
    }


    /**
     * Check if current authenticated User has the Role of System Admin.
     *
     * @param session  The HTTP session
     * @return true if User has System Admin Role, false otherwise
     */
    public static boolean isUserSystemAdmin(HttpSession session) {
        boolean retVal = false;
        if (null != session &&
            session.getAttribute("hpcUser") instanceof HpcUserDTO)
        {
            HpcUserDTO userDto = (HpcUserDTO) session.getAttribute("hpcUser");
            retVal = "SYSTEM_ADMIN".equals(userDto.getUserRole());
        }
        return retVal;
    }


    /**
     * Check if current authenticated User has the Role of System Admin or
     * the Role of Group Admin.
     *
     * @param session  The HTTP session
     * @return true if User has System Admin Role or Group Admin Role, false
     *          otherwise
     */
    public static boolean iUserSystemAdminOrGroupAdmin(HttpSession session) {
        boolean retVal = false;
        if (null != session &&
            session.getAttribute("hpcUser") instanceof HpcUserDTO)
        {
            HpcUserDTO userDto = (HpcUserDTO) session.getAttribute("hpcUser");
            retVal = "SYSTEM_ADMIN".equals(userDto.getUserRole()) ||
                     "GROUP_ADMIN".equals(userDto.getUserRole());
        }
        return retVal;
    }

    /**
     * Check if current authenticated User is a curator of any project
     *
     * @param session  The HTTP session
     * @return true if User is a curator, false otherwise
     */
    public static boolean isUserCurator(HttpSession session) {
        boolean retVal = false;
        if (null != session &&
            session.getAttribute("hpcUser") instanceof HpcUserDTO)
        {
            HpcUserDTO userDto = (HpcUserDTO) session.getAttribute("hpcUser");
            retVal = userDto.getDataCurator();
        }
        return retVal;
    }
}
