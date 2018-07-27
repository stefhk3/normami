package org.contentmine.ami.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.contentmine.cproject.util.CMineGlobber;
import org.contentmine.cproject.util.CMineUtil;
import org.contentmine.eucl.xml.XMLUtil;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import nu.xom.Element;
import nu.xom.Node;

/** creates occurrence and coocurrence stats
 * 
 * @author pm286
 *
 */
// FIXME put this into CorpusCache

public class OccurrenceAnalyzer {

	public enum OccurrenceType {
		BINOMIAL("binomial", "match"),
		STRING(null, "exact"), 
		GENE(null, "exact"), 
		;
		
		private String name;
		private String matchMethod;
		private GeneType geneType;
		
		private OccurrenceType(String name, String matchMethod) {
			this.name = name;
			this.matchMethod = matchMethod;
			this.geneType = null;
		}
		public String getMatchMethod() {
			return matchMethod;
		}
		public void setSubType(GeneType subType) {
			this.geneType = subType;
		}
	}

	public enum GeneType {
		HUMAN("human"),
		MOUSE("mouse"),
		DROSOPHILA("drosophila"),
		ARABIDOPSIS("arabidopsis"),
		;
		
		private String type;
		private GeneType(String type) {
			this.type = type;
		}
		
		public String getType() {
			return type;
		}
	}

	private List<Multiset.Entry<String>> resultsByImportance;
	private Map<String, Integer> serialByTermImportance;
	private Map<File, List<Multiset.Entry<String>>> entryListByFile;

	private OccurrenceType type = OccurrenceType.STRING;
	private String resultsDirRegex;
	private String code;
	private int maxCount;
	private List<File> resultsFiles;
	private List<File> cTreeFiles;
	private EntityAnalyzer entityAnalyzer;
	
	public OccurrenceAnalyzer() {
		setDefaults();
	}
	
	private void setDefaults() {
		type = OccurrenceType.STRING;
		maxCount = 20;
	}

	public OccurrenceAnalyzer setSearch(OccurrenceType type) {
		this.type = type;
		return this;
	}

	public OccurrenceAnalyzer setResultsDirRegex(String resultsDirRegex) {
		this.resultsDirRegex = resultsDirRegex;
		return this;
	}

	public String getAttName() {
		return type.getMatchMethod();
	}

	private List<Multiset.Entry<String>> getAllMatchedSpeciesInFileSortedByCount(File binomialFile) {
		Element element = XMLUtil.parseQuietlyToDocument(binomialFile).getRootElement();
		String attName = getAttName();
		List<Node> matches = XMLUtil.getQueryNodes(element, ".//*[local-name()='result']/@" + attName); // match expands species
		Multiset<String> multiSet = HashMultiset.create();
		for (Node match : matches) {
			multiSet.add(match.getValue());
		}
		List<Multiset.Entry<String>> entries = CMineUtil.getEntryListSortedByCount(multiSet);
		return entries;
	}

	public Map<File, List<Multiset.Entry<String>>> getOrCreateEntryListByCTreeFile() {
		if (entryListByFile == null) {
			entryListByFile = new HashMap<File, List<Multiset.Entry<String>>>();
			resultsFiles = getOrCreateResultsFiles();
			for (File resultsFile : resultsFiles) {
				List<Multiset.Entry<String>> resultsEntries = getAllMatchedSpeciesInFileSortedByCount(resultsFile);
				File cTreeFile = getCTreeFileAncestorFromResultsFile(resultsFile);
				entryListByFile.put(cTreeFile, resultsEntries);
			}
		}
		return entryListByFile;
	}

	public List<File> getOrCreateResultsFiles() {
		if (resultsFiles == null) {
			resultsFiles = new CMineGlobber().setRegex(resultsDirRegex).setLocation(getProjectDir()).listFiles();
		}
		return resultsFiles;
	}

	private File getProjectDir() {
		return entityAnalyzer == null ? null : entityAnalyzer.getProjectDir();
	}

	public List<Multiset.Entry<String>> getEntriesSortedByImportance() {
		getOrCreateEntryListByCTreeFile();
		Multiset<String> resultsMedianSet = getMedianMultiSet(entryListByFile);
		resultsByImportance = CMineUtil.getEntryListSortedByCount(resultsMedianSet);
		return resultsByImportance;
	}

	/** uses sqrt scaing for frequency.
	 * arbitrary
	 * 
	 * @param resultsSetListByFile
	 * @return
	 */
	private Multiset<String> getMedianMultiSet(Map<File, List<Entry<String>>> resultsSetListByFile) {
		Multiset<String> resultsSet = HashMultiset.create();
		for (List<Entry<String>> entryList : resultsSetListByFile.values()) {
			for (Entry<String> entry : entryList) {
				int newCount = (int) Math.ceil(Math.sqrt((double)entry.getCount()));
				resultsSet.add(entry.getElement(), newCount);
			}
		}
		return  resultsSet;
	}

	public OccurrenceAnalyzer setCode(String code) {
		this.code = code;
		return this;
	}

	public OccurrenceAnalyzer setMaxCount(int maxCount) {
		this.maxCount = maxCount;
		return this;
	}


	/** a simple lookup for ordering in matrixes, etc.
	 * 
	 * @param termsByImportance
	 * @return
	 */
	public Map<String, Integer> getOrCreateSerialByTermImportance() {
		if (serialByTermImportance == null) {
			serialByTermImportance = new HashMap<String, Integer>();
			for (Integer serial = 0; serial < resultsByImportance.size(); serial++) {
				Entry<String> termEntry = resultsByImportance.get(serial);
				String term = termEntry.getElement();
				serialByTermImportance.put(term, serial);
			}
		}
		return serialByTermImportance;
	}

	public List<File> getOrCreateCTreeFiles() {
		if (cTreeFiles == null) {
			cTreeFiles = new ArrayList<File>();
			getOrCreateResultsFiles();
			for (File resultsFile : resultsFiles) {
				File cTreeFile = getCTreeFileAncestorFromResultsFile(resultsFile);
				cTreeFiles.add(cTreeFile);
			}
		}
		return cTreeFiles;
	}

	private File getCTreeFileAncestorFromResultsFile(File resultsFile) {
		File cTreeFile = resultsFile.getParentFile().getParentFile().getParentFile().getParentFile();
		return cTreeFile;
	}

	public List<Integer> getSerialList(List<Entry<String>> termEntryList) {
		getOrCreateSerialByTermImportance();
		List<Integer> serialList = new ArrayList<Integer>();
		for (Entry<String> termEntry : termEntryList) {
			if (termEntry != null) {
				Integer serial = serialByTermImportance.get(termEntry.getElement());
				if (serial != null) {
					serialList.add(serial);
				}
			}
		}
		return serialList;
	}

	public int getMaxCount() {
		return maxCount;
	}

	public OccurrenceType getType() {
		return type;
	}

	public void setType(OccurrenceType type) {
		this.type = type;
	}

	public OccurrenceAnalyzer setEntityAnalyzer(EntityAnalyzer entityAnalyzer) {
		this.entityAnalyzer = entityAnalyzer;
		return this;
	}


}
