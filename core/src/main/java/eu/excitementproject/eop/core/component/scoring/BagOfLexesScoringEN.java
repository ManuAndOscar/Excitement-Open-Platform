package eu.excitementproject.eop.core.component.scoring;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;

import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;

import eu.excitementproject.eop.common.component.lexicalknowledge.LexicalResourceCloseException;
import eu.excitementproject.eop.common.component.lexicalknowledge.LexicalResourceException;
import eu.excitementproject.eop.common.component.lexicalknowledge.LexicalRule;
import eu.excitementproject.eop.common.component.lexicalknowledge.RuleInfo;
import eu.excitementproject.eop.common.component.scoring.ScoringComponentException;
import eu.excitementproject.eop.common.configuration.CommonConfig;
import eu.excitementproject.eop.common.configuration.NameValueTable;
import eu.excitementproject.eop.common.exception.ConfigurationException;
import eu.excitementproject.eop.core.component.lexicalknowledge.verb_ocean.RelationType;
import eu.excitementproject.eop.core.component.lexicalknowledge.verb_ocean.VerbOceanLexicalResource;
import eu.excitementproject.eop.core.component.lexicalknowledge.wordnet.WordnetLexicalResource;
import eu.excitementproject.eop.core.utilities.dictionary.wordnet.WordNetRelation;

/**
 * The <code>BagOfLexesScoring</code> class extends
 * <code>BagOfLemmasScoring</code>. It supports (currently) <code>WordNet</code>
 * and <code>VerbOcean</code> two lexical resources.
 * 
 * @author Rui Wang
 * @since January 2013
 */
public class BagOfLexesScoringEN extends BagOfLemmasScoring {

	static Logger logger = Logger
			.getLogger(BagOfLexesScoringEN.class.getName());

	/**
	 * the number of features
	 */
	private int numOfFeats = 0;

	@Override
	public int getNumOfFeats() {
		return numOfFeats;
	}

	private Set<WordnetLexicalResource> wnlrSet;

	private Set<VerbOceanLexicalResource> volrSet;
	
	/**
	 * the constructor using parameters
	 * 
	 * @param isWN whether to use <code>WordnetLexicalResource</code>
	 * @param WNRelations the array of WordNet relations
	 * @param isWNCollapsed whether to collapse the WordNet relations into one score
	 * @param useFirstSenseOnlyLeft whether to use the first sense only for the left-hand side
	 * @param useFirstSenseOnlyRight whether to use the first sense only for the right-hand side
	 * @param wnPath the file path to WordNet
	 * @param isVO whether to use <code>VerbOceanLexicalResource</code>
	 * @param VORelations the array of VerbOcean relations
	 * @param isVOCollapsed whether to collapse the VerbOcean relations into one score
	 * @param voPath the file path to VerbOcean
	 * @throws ConfigurationException
	 * @throws LexicalResourceException
	 */
	public BagOfLexesScoringEN(boolean isWN, String[] WNRelations, boolean isWNCollapsed, boolean useFirstSenseOnlyLeft, boolean useFirstSenseOnlyRight, String wnPath, boolean isVO, String[] VORelations, boolean isVOCollapsed, String voPath) throws ConfigurationException, LexicalResourceException {
		if (!isWN && !isVO) {
			throw new ConfigurationException(
					"Wrong configuation: didn't find any lexical resources for the BagOfLexesScoring component");
		}
		
		if (isWN) {
			if (null == WNRelations || 0 == WNRelations.length) {
				throw new ConfigurationException(
						"Wrong configuation: didn't find any relations for the WordNet");
			}
			Set<WordNetRelation> wnRelSet = new HashSet<WordNetRelation>();
			for (String relation : WNRelations) {
				if (relation.equalsIgnoreCase("HYPERNYM")) {
					wnRelSet.add(WordNetRelation.HYPERNYM);
				} else if (relation.equalsIgnoreCase("SYNONYM")) {
					wnRelSet.add(WordNetRelation.SYNONYM);
				} else if (relation.equalsIgnoreCase("PART_HOLONYM")) {
					wnRelSet.add(WordNetRelation.PART_HOLONYM);
				} else {
					logger.warning("Warning: wrong relation names for the WordNet");
				}
			}
			if (wnRelSet.isEmpty()) {
				throw new ConfigurationException(
						"Wrong configuation: didn't find any (correct) relations for the WordNet");
			}
			wnlrSet = new HashSet<WordnetLexicalResource>();
			File wnFile = new File(wnPath);			
			if (!wnFile.exists()) {
				throw new ConfigurationException("cannot find WordNet at: " + wnPath);
			}
			if (isWNCollapsed) {
				WordnetLexicalResource wnlr = new WordnetLexicalResource(wnFile, useFirstSenseOnlyLeft,
						useFirstSenseOnlyRight, wnRelSet);
				wnlrSet.add(wnlr);
				numOfFeats++;
			} else {
				for (WordNetRelation wnr : wnRelSet) {
					WordnetLexicalResource wnlr = new WordnetLexicalResource(
							wnFile, useFirstSenseOnlyLeft,
							useFirstSenseOnlyRight, Collections.singleton(wnr));
					wnlrSet.add(wnlr);
					numOfFeats++;
				}
			}
			logger.info("Load WordNet done.");
		}
		
		if (isVO) {
			if (null == VORelations || 0 == VORelations.length) {
				throw new ConfigurationException(
						"Wrong configuation: didn't find any relations for the VerbOcean");
			}
			Set<RelationType> voRelSet = new HashSet<RelationType>();
			for (String relation : VORelations) {
				if (relation.equalsIgnoreCase("strongerthan")) {
					voRelSet.add(RelationType.STRONGER_THAN);
				} else if (relation.equalsIgnoreCase("canresultin")) {
					voRelSet.add(RelationType.CAN_RESULT_IN);
				} else if (relation.equalsIgnoreCase("similar")) {
					voRelSet.add(RelationType.SIMILAR);
				} else {
					logger.warning("Warning: wrong relation names for the VerbOcean");
				}
			}
			if (voRelSet.isEmpty()) {
				throw new ConfigurationException(
						"Wrong configuation: didn't find any (correct) relations for the VerbOcean");
			}
			volrSet = new HashSet<VerbOceanLexicalResource>();
			File voFile = new File(voPath);
			if (!voFile.exists()) {
				throw new ConfigurationException("cannot find VerbOcean at: " + voPath);
			}
			if (isVOCollapsed) {
				VerbOceanLexicalResource volr = new VerbOceanLexicalResource(1,
						voFile, voRelSet);
				volrSet.add(volr);
				numOfFeats++;
			} else {
				for (RelationType vor : voRelSet) {
					VerbOceanLexicalResource volr = new VerbOceanLexicalResource(
							1, voFile, Collections.singleton(vor));
					volrSet.add(volr);
					numOfFeats++;
				}
			}
			logger.info("Load VerbOcean done.");
		}
	}

	/**
	 * the constructor using <code>CommonConfig</code>
	 * 
	 * @param config
	 *            the configuration
	 * @throws ConfigurationException
	 * @throws LexicalResourceException
	 */
	public BagOfLexesScoringEN(CommonConfig config)
			throws ConfigurationException, LexicalResourceException {
		NameValueTable comp = config.getSection("BagOfLexesScoring");

		if (null == comp.getString("WordnetLexicalResource")
				&& null == comp.getString("VerbOceanLexicalResource")) {
			throw new ConfigurationException(
					"Wrong configuation: didn't find any lexical resources for the BagOfLexesScoring component");
		}

		if (null != comp.getString("WordnetLexicalResource")) {
			String[] WNRelations = comp.getString("WordnetLexicalResource")
					.split(",");
			if (null == WNRelations || 0 == WNRelations.length) {
				throw new ConfigurationException(
						"Wrong configuation: didn't find any relations for the WordNet");
			}
			Set<WordNetRelation> wnRelSet = new HashSet<WordNetRelation>();
			for (String relation : WNRelations) {
				if (relation.equalsIgnoreCase("HYPERNYM")) {
					wnRelSet.add(WordNetRelation.HYPERNYM);
				} else if (relation.equalsIgnoreCase("SYNONYM")) {
					wnRelSet.add(WordNetRelation.SYNONYM);
				} else if (relation.equalsIgnoreCase("PART_HOLONYM")) {
					wnRelSet.add(WordNetRelation.PART_HOLONYM);
				} else {
					logger.warning("Warning: wrong relation names for the WordNet");
				}
			}
			if (wnRelSet.isEmpty()) {
				throw new ConfigurationException(
						"Wrong configuation: didn't find any (correct) relations for the WordNet");
			}
			boolean isCollapsed = true;
			boolean useFirstSenseOnlyLeft = false;
			boolean useFirstSenseOnlyRight = false;
			String wnPath = "/ontologies/EnglishWordNet-dict/";
			NameValueTable wnComp = config.getSection("WordnetLexicalResource");
			if (null != wnComp) {
				if (null != wnComp.getString("isCollapsed")
						&& !Boolean.parseBoolean(wnComp
								.getString("isCollapsed"))) {
					isCollapsed = false;
				}
				if (null != wnComp.getString("useFirstSenseOnlyLeft")
						&& Boolean.parseBoolean(wnComp
								.getString("useFirstSenseOnlyLeft"))) {
					useFirstSenseOnlyLeft = true;
				}
				if (null != wnComp.getString("useFirstSenseOnlyRight")
						&& Boolean.parseBoolean(wnComp
								.getString("useFirstSenseOnlyRight"))) {
					useFirstSenseOnlyRight = true;
				}
				if (null != wnComp.getString("wordNetFilesPath")) {
					wnPath = wnComp.getString("wordNetFilesPath");
				}
			}
			wnlrSet = new HashSet<WordnetLexicalResource>();
			File wnFile = new File(wnPath);			
			if (!wnFile.exists()) {
				throw new ConfigurationException("cannot find WordNet at: " + wnPath);
			}
			if (isCollapsed) {
				WordnetLexicalResource wnlr = new WordnetLexicalResource(wnFile, useFirstSenseOnlyLeft,
						useFirstSenseOnlyRight, wnRelSet);
				wnlrSet.add(wnlr);
				numOfFeats++;
			} else {
				for (WordNetRelation wnr : wnRelSet) {
					WordnetLexicalResource wnlr = new WordnetLexicalResource(
							wnFile, useFirstSenseOnlyLeft,
							useFirstSenseOnlyRight, Collections.singleton(wnr));
					wnlrSet.add(wnlr);
					numOfFeats++;
				}
			}
			logger.info("Load WordNet done.");
		}

		if (null != comp.getString("VerbOceanLexicalResource")) {
			String[] VORelations = comp.getString("VerbOceanLexicalResource")
					.split(",");
			if (null == VORelations || 0 == VORelations.length) {
				throw new ConfigurationException(
						"Wrong configuation: didn't find any relations for the VerbOcean");
			}
			Set<RelationType> voRelSet = new HashSet<RelationType>();
			for (String relation : VORelations) {
				if (relation.equalsIgnoreCase("strongerthan")) {
					voRelSet.add(RelationType.STRONGER_THAN);
				} else if (relation.equalsIgnoreCase("canresultin")) {
					voRelSet.add(RelationType.CAN_RESULT_IN);
				} else if (relation.equalsIgnoreCase("similar")) {
					voRelSet.add(RelationType.SIMILAR);
				} else {
					logger.warning("Warning: wrong relation names for the VerbOcean");
				}
			}
			if (voRelSet.isEmpty()) {
				throw new ConfigurationException(
						"Wrong configuation: didn't find any (correct) relations for the VerbOcean");
			}
			boolean isCollapsed = true;
			String voPath = "/VerbOcean/verbocean.unrefined.2004-05-20.txt";
			NameValueTable voComp = config
					.getSection("VerbOceanLexicalResource");
			if (null != voComp) {
				if (null != voComp.getString("isCollapsed")
						&& !Boolean.parseBoolean(voComp
								.getString("isCollapsed"))) {
					isCollapsed = false;
				}
				if (null != voComp.getString("verbOceanFilePath")) {
					voPath = voComp.getString("verbOceanFilePath");
				}
			}
			volrSet = new HashSet<VerbOceanLexicalResource>();
			File voFile = new File(voPath);
			if (!voFile.exists()) {
				throw new ConfigurationException("cannot find VerbOcean at: " + voPath);
			}
			if (isCollapsed) {
				VerbOceanLexicalResource volr = new VerbOceanLexicalResource(1,
						voFile, voRelSet);
				volrSet.add(volr);
				numOfFeats++;
			} else {
				for (RelationType vor : voRelSet) {
					VerbOceanLexicalResource volr = new VerbOceanLexicalResource(
							1, voFile, Collections.singleton(vor));
					volrSet.add(volr);
					numOfFeats++;
				}
			}
			logger.info("Load VerbOcean done.");
		}
	}

	@Override
	public String getComponentName() {
		return "BagOfLexesScoringEN";
	}

	/**
	 * close the component by closing the lexical resources.
	 */
	@Override
	public void close() throws ScoringComponentException {
		try {
			if (null != wnlrSet) {
				for (WordnetLexicalResource wnlr : wnlrSet) {
					wnlr.close();
				}
			}
			if (null != volrSet) {
				for (VerbOceanLexicalResource volr : volrSet) {
					volr.close();
				}
			}
		} catch (LexicalResourceCloseException e) {
			throw new ScoringComponentException(e.getMessage());
		}
	}

	@Override
	public Vector<Double> calculateScores(JCas aCas)
			throws ScoringComponentException {
		// 1) how many words of H (extended with multiple relations) can be
		// found in T divided by the length of H
		Vector<Double> scoresVector = new Vector<Double>();

		try {
			JCas tView = aCas.getView("TextView");
			HashMap<String, Integer> tBag = countTokens(tView);

			JCas hView = aCas.getView("HypothesisView");
			HashMap<String, Integer> hBag = countTokens(hView);

			if (null != wnlrSet && wnlrSet.size() != 0) {
				for (WordnetLexicalResource wnlr : wnlrSet) {
					scoresVector.add(calculateSingleLexScoreWithWNRelations(
							tBag, hBag, wnlr));
				}
			}
			if (null != volrSet && volrSet.size() != 0) {
				for (VerbOceanLexicalResource volr : volrSet) {
					scoresVector.add(calculateSingleLexScoreWithVORelations(
							tBag, hBag, volr));
				}
			}
		} catch (CASException e) {
			throw new ScoringComponentException(e.getMessage());
		}
		return scoresVector;
	}

	/**
	 * calculate the similarity score between T and H based on WordNet relations
	 * 
	 * @param tBag
	 *            the bag of words of T
	 * @param hBag
	 *            the bag of words of H
	 * @param wnlr
	 *            the WordNet relations
	 * @return the similarity score
	 * @throws ScoringComponentException
	 */
	protected double calculateSingleLexScoreWithWNRelations(
			HashMap<String, Integer> tBag, HashMap<String, Integer> hBag,
			WordnetLexicalResource wnlr) throws ScoringComponentException {
		double score = 0.0d;
		HashMap<String, Integer> tWordBag = new HashMap<String, Integer>();

		for (final Iterator<Entry<String, Integer>> iter = tBag.entrySet()
				.iterator(); iter.hasNext();) {
			Entry<String, Integer> entry = iter.next();
			final String word = entry.getKey();
			final int counts = entry.getValue().intValue();
			try {
				tWordBag.put(word, counts);
				for (LexicalRule<? extends RuleInfo> rule : wnlr
						.getRulesForLeft(word, null)) {
					if (tWordBag.containsKey(rule.getRLemma())) {
						int tmp = tWordBag.get(rule.getRLemma());
						tWordBag.put(rule.getRLemma(), tmp + counts);
					} else {
						tWordBag.put(rule.getRLemma(), counts);
					}
				}
			} catch (LexicalResourceException e) {
				throw new ScoringComponentException(e.getMessage());
			}
		}

		score = calculateSimilarity(tWordBag, hBag).get(0);

		return score;
	}

	/**
	 * calculate the similarity score between T and H based on VerbOcean
	 * relations
	 * 
	 * @param tBag
	 *            the bag of words of T
	 * @param hBag
	 *            the bag of words of H
	 * @param volr
	 *            the VerbOcean relations
	 * @return the similarity score
	 * @throws ScoringComponentException
	 */
	protected double calculateSingleLexScoreWithVORelations(
			HashMap<String, Integer> tBag, HashMap<String, Integer> hBag,
			VerbOceanLexicalResource volr) throws ScoringComponentException {
		double score = 0.0d;
		HashMap<String, Integer> tWordBag = new HashMap<String, Integer>();

		for (final Iterator<Entry<String, Integer>> iter = tBag.entrySet()
				.iterator(); iter.hasNext();) {
			Entry<String, Integer> entry = iter.next();
			final String word = entry.getKey();
			final int counts = entry.getValue().intValue();
			try {
				tWordBag.put(word, counts);
				for (LexicalRule<? extends RuleInfo> rule : volr
						.getRulesForLeft(word, null)) {
					if (tWordBag.containsKey(rule.getRLemma())) {
						int tmp = tWordBag.get(rule.getRLemma());
						tWordBag.put(rule.getRLemma(), tmp + counts);
					} else {
						tWordBag.put(rule.getRLemma(), counts);
					}
				}
			} catch (LexicalResourceException e) {
				throw new ScoringComponentException(e.getMessage());
			}
		}

		score = calculateSimilarity(tWordBag, hBag).get(0);

		return score;
	}
}
