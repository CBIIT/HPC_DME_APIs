/**
 * HpcUserDetailsServiceImpl.java
 *
 * Copyright SVG, Inc.
 * Copyright Leidos Biomedical Research, Inc
 * 
 * Distributed under the OSI-approved BSD 3-Clause License.
 * See http://ncip.github.com/HPC/LICENSE.txt for details.
 */

package gov.nih.nci.hpc.ws.rs.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;


/**
 * <p>
 * HPC Project REST Service Implementation.
 * </p>
 *
 * @author <a href="mailto:eran.rosenberg@nih.gov">Eran Rosenberg</a>
 * @version $Id$
 */

public class HpcUserDetailsServiceImpl implements UserDetailsService
{   
    
    //---------------------------------------------------------------------//
    // Methods
    //---------------------------------------------------------------------//
    
    //---------------------------------------------------------------------//
    // HpcDatasetRestService Interface Implementation
    //---------------------------------------------------------------------//  
	
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException
    {
    	List<SimpleGrantedAuthority> authorities = new ArrayList<SimpleGrantedAuthority>();
    	SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_CUSTOMER");
    	authorities.add(authority);
    	User user = new User("id", "pwd", authorities);
    	System.err.println("user :" + user);
    	return user;
    	
    }
    
} 