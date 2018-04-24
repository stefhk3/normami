package org.contentmine.ami.plugins;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.contentmine.CHESConstants;
import org.contentmine.cproject.testutil.DataTablesToolAnalyzer;
import org.contentmine.cproject.util.CMineTestFixtures;
import org.contentmine.norma.NAConstants;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("long")
public class CommandProcessorIT {

	private static final Logger LOG = Logger.getLogger(CommandProcessorIT.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	private final static String DICTIONARY_RESOURCE = CHESConstants.ORG_CM+"/"+NAConstants.AMI+"/plugins/dictionary";
	
	
	@Test
	public void testCommandLineSearch() throws IOException {
		String project = "zika10";
		File rawDir = new File(NAConstants.TEST_AMI_DIR, project);
		File projectDir = new File("target/tutorial1/"+project);
		CMineTestFixtures.cleanAndCopyDir(rawDir, projectDir);
		String cmd = ""
				// symbol/root
				+ " search(disease)"
				// resource
				+ " search("+DICTIONARY_RESOURCE+"/inn.xml)"
				// file
				+ " search(src/main/resources"+"/"+DICTIONARY_RESOURCE+"/tropicalVirus.xml)"
				// URL - will fail if not on net
//				+ " search(https://raw.githubusercontent.com/ContentMine/dictionaries/master/xml/epidemic.xml)"
	    ;
		String[] args = (projectDir+" "+cmd).split("\\s+");
		CommandProcessor.main(args);
		new DataTablesToolAnalyzer(new File(projectDir, "full.dataTables.html"))
				.assertRowCount(9)
				.assertColumnCount(1+3) // have to count row label columns
				.assertColumnHeadings("[articles, dic:disease, dic:inn, dic:tropicalVirus]")
				.assertCellValue(2, 3, "Zika x 2")
				;
	}

	@Test
	public void testCommandLineShort() throws IOException {
		String project = "zika10";
		File projectDir = new File("target/tutorial/"+project);
		File rawDir = new File(NAConstants.TEST_AMI_DIR, project);
		CMineTestFixtures.cleanAndCopyDir(rawDir, projectDir);
		String cmd = ""
		+ "word(frequencies)xpath:@count>20~w.stopwords:pmcstop.txt_stopwords.txt"
		+ " sequence(dnaprimer)"
		+ " gene(human) "
		+ " search(tropicalVirus)"
	    ;
		CommandProcessor.main((projectDir+" "+cmd).split("\\s+"));
	}

	@Test
	public void testCommandLineShort1() throws IOException {
		String project = "zika10";
		File projectDir = new File("target/tutorial/"+project);
		File rawDir = new File(NAConstants.TEST_AMI_DIR, project);
		CMineTestFixtures.cleanAndCopyDir(rawDir, projectDir);
		String cmd = ""
		// other ways of locating stopwords
		+ "word(frequencies)xpath:@count>20~w.stopwords:"+
		NAConstants.SLASH_AMI_RESOURCE+"/wordutil/pmcstop_https://raw.githubusercontent.com/ContentMine/ami/master/src/main/resources/org/contentmine/ami/wordutil/stopwords.txt"
		+ " sequence(dnaprimer)"
		+ " gene(human) "
		+ " search(tropicalVirus)"
	    ;
		CommandProcessor.main((projectDir+" "+cmd).split("\\s+"));
	}

	@Test
	public void testCommandLineShort2() throws IOException {
		String project = "zika10";
		File projectDir = new File("target/tutorial/"+project);
		File rawDir = new File(NAConstants.TEST_AMI_DIR, project);
		CMineTestFixtures.cleanAndCopyDir(rawDir, projectDir);
		String cmd = ""
		+ " word(20,pmcstop.txt,stopwords.txt)"
		+ " sequence(dnaprimer)"
		+ " gene(human) "
		+ " search(tropicalVirus)"
	    ;
		CommandProcessor.main((projectDir+" "+cmd).split("\\s+"));
	}

	/** preprocessor - may not be good idea.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testCommandLinePreprocessor() throws IOException {
		String project = "zika10";
		File projectDir = new File("target/tutorial/"+project);
		File rawDir = new File(NAConstants.TEST_AMI_DIR, project);
		CMineTestFixtures.cleanAndCopyDir(rawDir, projectDir);
		String cmd = ""
		+ "w_fstop"
		+ " sq_d"
		+ " g_h"
		+ " s_tv"
		+ " s_inn"
		+ " s_nal"
		+ " s_phch"
		
	    ;
		CommandProcessor.main((projectDir+" "+cmd).split("\\s+"));
	}

	@Test
	//@Ignore
	// runs defaults
	public void testCommandLineShortEmpty() throws IOException {
		String project = "zika10";
		File projectDir = new File("target/tutorial/"+project);
		File rawDir = new File(NAConstants.TEST_AMI_DIR, project);
		CMineTestFixtures.cleanAndCopyDir(rawDir, projectDir);
		CommandProcessor.main(new String[]{projectDir.toString()});
	}

	@Test
//	@Ignore // LONG
	public void testCommandLine() throws IOException {
		String project = "zika10";
		File projectDir = new File("target/tutorial/"+project);
		File rawDir = new File(NAConstants.TEST_AMI_DIR, project);
		CMineTestFixtures.cleanAndCopyDir(rawDir, projectDir);
		String cmd = ""
		+ "word(frequencies)xpath:@count>20~w.stopwords:pmcstop.txt_stopwords.txt"
		+ " sequence(dnaprimer)"
		+ " gene(human) "
		+ " word(search)w.search:"+DICTIONARY_RESOURCE+"/tropicalVirus.xml"
	    ;
		CommandProcessor.main((projectDir+" "+cmd).split("\\s+"));
	}

	@Test
	public void  testHindawiSampleMini() throws IOException {
		File rawDir = new File(NAConstants.TEST_AMI_DIR, "hindawiepmc");
		File projectDir = new File("target/tutorial/hindawi/samplemini");
		CMineTestFixtures.cleanAndCopyDir(rawDir, projectDir);
		String cmd = ""
		+ " word(search)w.search:"+DICTIONARY_RESOURCE+"/disease.xml"
	    ;
		CommandProcessor.main((projectDir+" "+cmd).split("\\s+"));
	}
		
	@Test
	/** ca 100 files
	 * 
	 * works
	 * 
	 * @throws IOException
	 */
	public void  testHindawiEPMC() throws IOException {
		
		File rawDir = new File(NAConstants.TEST_AMI_DIR, "hindawiepmc");
		File projectDir = new File("target/tutorial/hindawi/epmc");
		CMineTestFixtures.cleanAndCopyDir(rawDir, projectDir);
		String cmd = "word(frequencies)xpath:@count>20~w.stopwords:pmcstop.txt_stopwords.txt"
		+ " sequence(dnaprimer)"
		+ " species(binomial)"
		+ " gene(human) "
		+ " word(search)w.search:"+DICTIONARY_RESOURCE+"/disease.xml"
		+ " word(search)w.search:"+DICTIONARY_RESOURCE+"/phytochemicals2.xml"
		+ " word(search)w.search:"+DICTIONARY_RESOURCE+"/inn.xml"
		+ " search(tropicalVirus)"

	    ;
		CommandProcessor.main((projectDir+" "+cmd).split("\\s+"));
			
	}
}
