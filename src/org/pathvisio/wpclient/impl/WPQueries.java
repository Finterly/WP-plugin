// PathVisio WP Client
// Plugin that provides a WikiPathways client for PathVisio.
// Copyright 2013 developed for Google Summer of Code
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License. 
// You may obtain a copy of the License at 
// 
// http://www.apache.org/licenses/LICENSE-2.0 
//  
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
// See the License for the specific language governing permissions and 
// limitations under the License.
//
package org.pathvisio.wpclient.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.bridgedb.bio.Organism;
import org.pathvisio.libgpml.io.ConverterException;
import org.pathvisio.libgpml.model.PathwayModel;
import org.pathvisio.core.preferences.PreferenceManager;
import org.pathvisio.core.util.ProgressKeeper;
import org.pathvisio.wikipathways.webservice.WSCurationTag;
import org.pathvisio.wikipathways.webservice.WSPathway;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.pathvisio.wikipathways.webservice.WSSearchResult;
import org.pathvisio.wpclient.FailedConnectionException;
import org.pathvisio.wpclient.IWPQueries;
import org.pathvisio.wpclient.preferences.URLPreference;
import org.wikipathways.client.WikiPathwaysClient;


/**
 * WP Queries implementation
 * These functions can be used by other plugins by using the
 * OSGi service IWPQueries
 * 
 * @author Martina Kutmon
 *
 */
public class WPQueries implements IWPQueries {
		
	private WikiPathwaysClient wpClient; 
	private String currentUrl;
	
	private WikiPathwaysClient getClient() throws FailedConnectionException {
		if(wpClient == null || !currentUrl.equals(PreferenceManager.getCurrent().get(URLPreference.CONNECTION_URL))) {
			try {
				wpClient = new WikiPathwaysClient(new URL(PreferenceManager.getCurrent().get(URLPreference.CONNECTION_URL)));
				currentUrl = PreferenceManager.getCurrent().get(URLPreference.CONNECTION_URL);
			} catch (MalformedURLException e) {
				throw new FailedConnectionException("Can not connect to WikiPathways.\nInvalid URL.");
			}
		}
		return wpClient;
	}
	
	/**
	 * retrieves all pathways from wikipathways
	 */
	@Override
	public Set<WSPathwayInfo> browseAll(ProgressKeeper pk) throws RemoteException, FailedConnectionException {
		WikiPathwaysClient client = getClient();
		Set<WSPathwayInfo> set = new HashSet<WSPathwayInfo>();

		if(pk != null) pk.setTaskName("Browsing WikiPathways");
		WSPathwayInfo[] result = client.listPathways();
		set.addAll(Arrays.asList(result));
		
		return set;
	}

	/**
	 * retrieves all pathways for one organism from wikipathways
	 */
	@Override
	public Set<WSPathwayInfo> browseByOrganism(Organism organism, ProgressKeeper pk) throws RemoteException, FailedConnectionException {
		WikiPathwaysClient client = getClient();
		Set<WSPathwayInfo> set = new HashSet<WSPathwayInfo>();

		if(pk != null) pk.setTaskName("Browse WikiPathways");
		if(pk != null) pk.report("Get pathways for species " + organism.latinName());
		
		WSPathwayInfo[] result = client.listPathways(organism);
		set.addAll(Arrays.asList(result));
		
		return set;
	}

	/**
	 * retrieves all pathways tags with a curation tag from wikipathways
	 */
	@Override
	public Set<WSPathwayInfo> browseByCurationTag(String curationTag, ProgressKeeper pk) throws RemoteException, FailedConnectionException {
		WikiPathwaysClient client = getClient();
		Set<WSPathwayInfo> set = new HashSet<WSPathwayInfo>();

		if(pk != null) pk.setTaskName("Browse WikiPathways");
		if(pk != null) pk.report("Get pathways with curation tag: " + curationTag);
		
		WSCurationTag[] result = client.getCurationTagsByName(curationTag);
		for (WSCurationTag tag : result) {
			set.add(tag.getPathway());
		}
		
		return set;
	}

	/**
	 * retrieves all pathways from a specific organism that are tags with 
	 * a sepcific curation tag from wikipathways
	 */
	@Override
	public Set<WSPathwayInfo> browseByOrganismAndCurationTag(Organism organism, String curationTag, ProgressKeeper pk) throws RemoteException, FailedConnectionException {
		Set<WSPathwayInfo> set = new HashSet<WSPathwayInfo>();
		
		if(pk != null) pk.setTaskName("Browse WikiPathways");
		if(pk != null) pk.report("Get pathways with curation tag " + curationTag);
		Set<WSPathwayInfo> pwyCurTag = browseByCurationTag(curationTag,pk);
		if(pk != null) pk.report("Filter pathways for species " + organism.latinName());
		
		for (WSPathwayInfo info : pwyCurTag) {
			if (info.getSpecies().equals(organism.latinName())) {
				set.add(info);
			}
		}
		return set;
	}

	/**
	 * retrieves a list of organisms from wikipathways
	 */
	@Override
	public List<String> listOrganisms(ProgressKeeper pk) throws RemoteException, FailedConnectionException {
		WikiPathwaysClient client = getClient();
		if(pk != null) pk.setTaskName("Test connection to WikiPathways");
		if(pk != null) pk.report("Get list of organisms from WikiPathways");
		String [] organisms = client.listOrganisms();
		return Arrays.asList(organisms);
	}
	
	/**
	 * retrieves a list of curation tags for a specific pathway
	 */
	@Override
	public Set<WSCurationTag> getCurationTags(String pwId, ProgressKeeper pk) throws RemoteException, FailedConnectionException {
		WikiPathwaysClient client = getClient();
		if(pk != null) pk.setTaskName("Retrieve curation tag");
		if(pk != null) pk.report("Get curation tags for pathway " + pwId);
		WSCurationTag [] tags = client.getCurationTags(pwId);
		return new HashSet<WSCurationTag>(Arrays.asList(tags));
	}

	/**
	 * finds all pathways by a text query
	 */
	@Override
	public WSSearchResult[] findByText(String text, ProgressKeeper pk) throws RemoteException, FailedConnectionException  {
		WikiPathwaysClient client = getClient();
		if(pk != null) pk.setTaskName("Search for \"" + text + "\".");
		WSSearchResult [] result = client.findPathwaysByText(text);
		return result;
	}
		
	/**
	 * gets a specific pathway by id and revision
	 */
	@Override
	public WSPathway getPathway(String id, Integer revision, ProgressKeeper pk) throws RemoteException, FailedConnectionException, ConverterException {
		WikiPathwaysClient client = getClient();
		if(pk != null) pk.setTaskName("Get pathway " + id + ".");
		WSPathway pathway = client.getPathway(id, revision);
		return pathway;
	}
		 
	/**
	 * finds all pathways by a text query for a specific organism
	 */
	@Override
	public WSSearchResult[] findByTextInOrganism(String text, Organism organism, ProgressKeeper pk) throws RemoteException, FailedConnectionException {
		WikiPathwaysClient client = getClient();
		if(pk != null) pk.setTaskName("Search for \"" + text + "\" in " + organism.latinName() + " pathways.");
		WSSearchResult [] result = client.findPathwaysByText(text, organism);
		return result;
	}
	
	/**
	 * finds all pathways by literature reference
	 */
	@Override
	public WSSearchResult[] findByLiteratureReference(String reference, ProgressKeeper pk) throws RemoteException, FailedConnectionException {
		WikiPathwaysClient client = getClient();
		if(pk != null) pk.setTaskName("Search for literature reference \"" + reference + "\".");
		WSSearchResult [] result = client.findPathwaysByLiterature(reference);
		return result;
	}
		
	/**
	 * login necessary to upload new data
	 */
	@Override
	public void login(String username, String password) throws RemoteException, FailedConnectionException {
		WikiPathwaysClient client = getClient();
		client.login(username, password);
	}

	/**
	 * upload a new pathway
	 */
	@Override
	public WSPathwayInfo uploadPathway(PathwayModel pathway) throws RemoteException, FailedConnectionException, ConverterException {
		WikiPathwaysClient client = getClient();
		return client.createPathway(pathway);
	}
	
	/**
	 * update a pathway
	 */
	@Override
	public void updatePathway(PathwayModel pathway, String id, Integer revision, String description) throws RemoteException, FailedConnectionException, ConverterException {
		WikiPathwaysClient client = getClient();
		client.updatePathway(id, pathway, description, revision);
	}

	/**
	 * updates a curation tag of a specific pathway
	 */
	@Override
	public void updateCurationTag(String tag, String id, String description, int revision)
			throws RemoteException, FailedConnectionException,
			ConverterException {
		WikiPathwaysClient client = getClient();
		client.saveCurationTag(id, tag, description, revision);
	}

	/**
	 * gets the pathway info for a specific pathway
	 */
	@Override
	public WSPathwayInfo getPathwayInfo(String id, ProgressKeeper pk)
			throws RemoteException, FailedConnectionException,
			ConverterException {
		WikiPathwaysClient client = getClient();
		if(pk != null) pk.setTaskName("Get pathway " + id + ".");
		WSPathwayInfo pathway = client.getPathwayInfo(id);
		return pathway;
	}

	/**
	 * find pathways by a list of xrefs
	 */
	@Override
	public WSSearchResult[] findByXref(Xref[] xrefs, ProgressKeeper pk) throws RemoteException, FailedConnectionException, ConverterException {
		WikiPathwaysClient client = getClient();
		if(pk != null) pk.setTaskName("Find pathways containing\nxrefs.");
		WSSearchResult[] results = client.findPathwaysByXref(xrefs);
		return results;
	}

	/**
	 * gets xref list for a pathway
	 */
	@Override
	public String[] getXrefList(String pwId, DataSource ds, ProgressKeeper pk) throws RemoteException, FailedConnectionException {
		WikiPathwaysClient client = getClient();
		if(pk != null) pk.setTaskName("Retrieve Xref List");
		return client.getXrefList(pwId, ds);
	}
}
