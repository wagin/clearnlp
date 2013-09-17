/**
* Copyright 2012-2013 University of Massachusetts Amherst
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.clearnlp.tokenization;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import jregex.MatchResult;
import jregex.Replacer;
import jregex.Substitution;
import jregex.TextBuffer;

import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import com.clearnlp.dictionary.DTTokenizer;
import com.clearnlp.morphology.MPLib;
import com.clearnlp.pattern.PTLib;
import com.clearnlp.pattern.PTLink;
import com.clearnlp.util.UTArray;
import com.clearnlp.util.UTInput;
import com.clearnlp.util.pair.IntIntPair;
import com.clearnlp.util.pair.Pair;
import com.clearnlp.util.pair.StringBooleanPair;

/**
 * @since 1.1.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
public class EnglishTokenizer extends AbstractTokenizer
{
	protected final String S_DELIM			= " ";
	protected final String S_PROTECTED		= "PR0T_";
	protected final String S_D0D			= "_DPPD_";
	protected final String S_HYPHEN			= "_HYYN_";
	protected final String S_AMPERSAND		= "_APSD_";
	protected final String S_APOSTROPHY		= "_AOOR_";
	protected final String S_PERIOD			= "_PERI_";
	protected final int    N_PROTECTED		= S_PROTECTED.length();
	
	protected final Pattern  P_DELIM		= Pattern.compile(S_DELIM);
	protected final Pattern  P_HYPHEN		= Pattern.compile("-");
	protected final Pattern  P_ABBREVIATION	= Pattern.compile("^(\\p{Alpha}\\.)+\\p{Alpha}?$");
	protected final String[] A_D0D			= {".",",",":","-","/","'"};
	
	protected Replacer   R_URL;
	protected Replacer   R_ABBREVIATION;
	protected Replacer   R_PERIOD_LIKE;
	protected Replacer   R_PERIOD;
	protected Replacer   R_MARKER;
	protected Replacer   R_APOSTROPHY;
	protected Replacer   R_USDOLLAR;
	protected Replacer   R_AMPERSAND;
	protected Replacer   R_WAW;
	protected Replacer   R_PUNCTUATION_PRE;
	protected Replacer   R_PUNCTUATION_POST;
	protected Replacer[] R_D0D;
	protected Replacer[] R_UNIT;
	
	protected List<Pair<String,Pattern>>	L_NON_UTF8;
	protected Set<String>					T_EMOTICONS;
	protected Set<String>					T_ABBREVIATIONS;
	protected Pattern						P_HYPHEN_LIST;
	protected ObjectIntOpenHashMap<String>	M_D0D;
	protected ObjectIntOpenHashMap<String>	M_COMPOUNDS;
	protected List<IntIntPair[]>			L_COMPOUNDS;
	protected Pattern[]						P_RECOVER_D0D;
	protected Pattern						P_RECOVER_DOT;
	protected Pattern						P_RECOVER_PERIOD;
	protected Pattern						P_RECOVER_HYPHEN;
	protected Pattern						P_RECOVER_APOSTROPHY;
	protected Pattern						P_RECOVER_AMPERSAND;
	
	public EnglishTokenizer()
	{
		initReplacers();
		initMapsD0D();
		initPatterns();
		initDictionaries();
	}
	
	public List<StringBooleanPair> getTokenList(String str)
	{
		str = normalizeNonUTF8(str);
		List<StringBooleanPair> lTokens = tokenizeWhiteSpaces(str);

		protectEmoticons(lTokens);
		lTokens = tokenizePatterns(lTokens, R_URL);
		lTokens = tokenizePatterns(lTokens, R_ABBREVIATION);
		lTokens = tokenizePatterns(lTokens, R_PERIOD_LIKE);
		lTokens = tokenizePatterns(lTokens, R_MARKER);
		lTokens = tokenizePatterns(lTokens, R_USDOLLAR);
		for (Replacer r : R_D0D) replaceProtects(lTokens, r);
		replaceHyphens(lTokens);
		lTokens = tokenizePatterns(lTokens, R_PUNCTUATION_PRE);
		protectAbbreviations(lTokens);
		protectFilenames(lTokens);
		if (b_twit)	protectTwits(lTokens);
		
		lTokens = tokenizeCompounds(lTokens);
		lTokens = tokenizePatterns(lTokens, R_APOSTROPHY);
		if (b_userId) replaceProtects(lTokens, R_PERIOD);
		replaceProtects(lTokens, R_AMPERSAND);
		replaceProtects(lTokens, R_WAW);
		for (Replacer r : R_UNIT) lTokens = tokenizePatterns(lTokens, r);
		lTokens = tokenizePatterns(lTokens, R_PUNCTUATION_POST);
		
		int i, size = P_RECOVER_D0D.length;
		for (i=0; i<size; i++)	recoverPatterns(lTokens, P_RECOVER_D0D[i], A_D0D[i]);
		if (b_userId) recoverPatterns(lTokens, P_RECOVER_PERIOD, ".");
		recoverPatterns(lTokens, P_RECOVER_HYPHEN, "-");
		recoverPatterns(lTokens, P_RECOVER_APOSTROPHY, "'");
		recoverPatterns(lTokens, P_RECOVER_AMPERSAND, "&");
		return lTokens;
	}
	
	/** Called by {@link EnglishTokenizer#EnglishTokenizer(ZipInputStream)}. */
	private void initReplacers()
	{
		R_URL          = PTLink.URL_SPAN.replacer(new SubstitutionOne());
		R_ABBREVIATION = new jregex.Pattern("(^(\\p{Alpha}\\.)+)(\\p{Punct}*$)").replacer(new SubstitutionOnePlus());
		R_PERIOD_LIKE  = new jregex.Pattern("(\\.|\\?|\\!){2,}").replacer(new SubstitutionOne());
		R_MARKER       = new jregex.Pattern("\\-{2,}|\\*{2,}|\\={2,}|\\~{2,}|\\,{2,}|\\`{2,}|\\'{2,}").replacer(new SubstitutionOne());
		R_APOSTROPHY   = new jregex.Pattern("(?i)((\\')(s|d|m|z|ll|re|ve|nt)|n(\\')t)$").replacer(new SubstitutionOne());
		R_USDOLLAR     = new jregex.Pattern("^US\\$").replacer(new SubstitutionOne());
		R_AMPERSAND    = getReplacerAmpersand();
		R_WAW          = getReplacerWAWs();
		R_PERIOD       = getReplacerPeriods();
		R_PUNCTUATION_PRE  = new jregex.Pattern("\\(|\\)|\\[|\\]|\\{|\\}|<|>|\\,|\\:|\\;|\\\"").replacer(new SubstitutionOne());
		R_PUNCTUATION_POST = new jregex.Pattern("\\.|\\?|\\!|\\`|\\'|\\-|\\/|\\@|\\#|\\$|\\%|\\&|\\|").replacer(new SubstitutionOne());
		
		initReplacersD0Ds();
	}
	
	private Replacer getReplacerAmpersand()
	{
		return new jregex.Pattern("(\\p{Upper})(\\&)(\\p{Upper})").replacer(new Substitution()
		{
			@Override
			public void appendSubstitution(MatchResult match, TextBuffer dest)
			{
				dest.append(match.group(1));
				dest.append(S_AMPERSAND);
				dest.append(match.group(3));
			}
		});
	}
	
	/** Called by {@link EnglishTokenizer#initReplacers()}. */
	private Replacer getReplacerWAWs()
	{
		return new jregex.Pattern("(\\w)(\\')(\\w)").replacer(new Substitution()
		{
			@Override
			public void appendSubstitution(MatchResult match, TextBuffer dest)
			{
				dest.append(match.group(1));
				dest.append(S_APOSTROPHY);
				dest.append(match.group(3));
			}
		});
	}
	
	private Replacer getReplacerPeriods()
	{
		return new jregex.Pattern("(\\p{Alnum})(\\.)(\\p{Alnum})").replacer(new Substitution()
		{
			@Override
			public void appendSubstitution(MatchResult match, TextBuffer dest)
			{
				dest.append(match.group(1));
				dest.append(S_PERIOD);
				dest.append(match.group(3));
			}
		});
	}
	
	/** Called by {@link EnglishTokenizer#initReplacers()}. */
	private void initReplacersD0Ds()
	{
		String[] regex = {"(^|\\p{Alnum})(\\.)(\\d)", "(\\d)(,|:|-|\\/)(\\d)", "(^)(\\')(\\d)", "(\\d)(\\')(s)"};
		int i, size = regex.length;
		
		R_D0D = new Replacer[size];
		
		for (i=0; i<size; i++)
			R_D0D[i] = new jregex.Pattern(regex[i]).replacer(new SubstitutionD0D());
	}
	
	/** Called by {@link EnglishTokenizer#EnglishTokenizer(ZipInputStream)}. */
	private void initMapsD0D()
	{
		M_D0D = new ObjectIntOpenHashMap<String>();
		int i, size = A_D0D.length;
		
		for (i=0; i<size; i++)
			M_D0D.put(A_D0D[i], i);
	}
	
	private void initPatterns()
	{
		int i, size = A_D0D.length;
		P_RECOVER_D0D = new Pattern[size];
		
		for (i=0; i<size; i++)
			P_RECOVER_D0D[i] = Pattern.compile(S_D0D+i+"_");
		
		P_RECOVER_PERIOD     = Pattern.compile(S_PERIOD);
		P_RECOVER_HYPHEN     = Pattern.compile(S_HYPHEN);
		P_RECOVER_APOSTROPHY = Pattern.compile(S_APOSTROPHY);
		P_RECOVER_AMPERSAND  = Pattern.compile(S_AMPERSAND);
		
	}
	
	private void initDictionaries()
	{
		try
		{
			T_EMOTICONS     = UTInput.getStringSet(UTInput.getInputStreamsFromClasspath(DTTokenizer.EMOTICONS));
			T_ABBREVIATIONS = UTInput.getStringSet(UTInput.getInputStreamsFromClasspath(DTTokenizer.ABBREVIATIONS));
			P_HYPHEN_LIST   = getHyphenPatterns(UTInput.getInputStreamsFromClasspath(DTTokenizer.HYPHENS));
			initDictionariesComounds(UTInput.getInputStreamsFromClasspath(DTTokenizer.COMPOUNDS));
			initDictionariesUnits(UTInput.getInputStreamsFromClasspath(DTTokenizer.UNITS));
			initDictionaryNonUTF8(UTInput.getInputStreamsFromClasspath(DTTokenizer.NON_UTF8));
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
/*	private void initDictionaries(ZipInputStream in)
	{
		String filename;
		ZipEntry entry;

		try
		{
			while ((entry = in.getNextEntry()) != null)
			{
				filename = entry.getName();
				
				     if (filename.equals(DTTokenizer.EMOTICONS))
				    T_EMOTICONS     = UTInput.getStringSet(in);
				else if (filename.equals(DTTokenizer.ABBREVIATIONS))
					T_ABBREVIATIONS = UTInput.getStringSet(in);
				else if (filename.equals(DTTokenizer.HYPHENS))
					P_HYPHEN_LIST   = getHyphenPatterns(in);
				else if (filename.equals(DTTokenizer.COMPOUNDS))
					initDictionariesComounds(in);
				else if (filename.equals(DTTokenizer.UNITS))
					initDictionariesUnits(in);
				else if (filename.equals(DTTokenizer.NON_UTF8))
					initDictionaryNonUTF8(in);
			}
			
			in.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}*/
	
	/** Called by {@link EnglishTokenizer#initDictionaries(ZipInputStream)}. */
	private Pattern getHyphenPatterns(InputStream in) throws Exception
	{
		BufferedReader fin = new BufferedReader(new InputStreamReader(in));
		StringBuilder build = new StringBuilder();
		String line;
		
		while ((line = fin.readLine()) != null)
		{
			build.append("|");
			build.append(line.trim());
		}
		
		return Pattern.compile(build.substring(1));
	}
	
	/** Called by {@link EnglishTokenizer#initDictionaries(ZipInputStream)}. */
	private void initDictionariesComounds(InputStream in) throws Exception
	{
		BufferedReader fin = new BufferedReader(new InputStreamReader(in));
		M_COMPOUNDS = new ObjectIntOpenHashMap<String>();
		L_COMPOUNDS = new ArrayList<IntIntPair[]>();
		
		int i, j, len, bIdx, eIdx;
		IntIntPair[] p;
		String[] tmp;
		String line;
		
		for (i=1; (line = fin.readLine()) != null; i++)
		{
			tmp = P_DELIM.split(line.trim());
			len = tmp.length;
			p   = new IntIntPair[len];
			
			M_COMPOUNDS.put(UTArray.join(tmp, ""), i);
			L_COMPOUNDS.add(p);
			
			for (j=0,bIdx=0; j<len; j++)
			{
				eIdx = bIdx + tmp[j].length();
				p[j] = new IntIntPair(bIdx, eIdx);
				bIdx = eIdx;
			}
		}
	}
	
	/** Called by {@link EnglishTokenizer#initDictionaries(ZipInputStream)}. */
	private void initDictionariesUnits(InputStream in) throws Exception
	{
		BufferedReader fin = new BufferedReader(new InputStreamReader(in));
		String signs       = fin.readLine().trim();
		String currencies  = fin.readLine().trim();
		String units       = fin.readLine().trim();
		
		R_UNIT = new Replacer[4];
		
		R_UNIT[0] = new jregex.Pattern("^(?i)(\\p{Punct}*"+signs+")(\\d)").replacer(new SubstitutionTwo());
		R_UNIT[1] = new jregex.Pattern("^(?i)(\\p{Punct}*"+currencies+")(\\d)").replacer(new SubstitutionTwo());
		R_UNIT[2] = new jregex.Pattern("(?i)(\\d)("+currencies+"\\p{Punct}*)$").replacer(new SubstitutionTwo());
		R_UNIT[3] = new jregex.Pattern("(?i)(\\d)("+units+"\\p{Punct}*)$").replacer(new SubstitutionTwo());
	}
	
	private void initDictionaryNonUTF8(InputStream in) throws Exception
	{
		BufferedReader fin = new BufferedReader(new InputStreamReader(in));
		Pattern tab = Pattern.compile("\t");
		String line;
		String[] t;
		
		L_NON_UTF8 = new ArrayList<Pair<String,Pattern>>();
		
		while ((line = fin.readLine()) != null)
		{
			t = tab.split(line);
			L_NON_UTF8.add(new Pair<String,Pattern>(t[1], Pattern.compile(t[0])));
		}
	}
	
	protected String normalizeNonUTF8(String str)
	{
		for (Pair<String,Pattern> p : L_NON_UTF8)
			str = p.o2.matcher(str).replaceAll(p.o1);
		
		return str;
	}
	
	/** Called by {@link EnglishTokenizer#getTokenList(String)}. */
	protected List<StringBooleanPair> tokenizeWhiteSpaces(String str)
	{
		List<StringBooleanPair> tokens = new ArrayList<StringBooleanPair>();
		
		for (String token : PTLib.splitWhiteSpaces(str))
			tokens.add(new StringBooleanPair(token, false));
		
		return tokens;
	}
	
	/** Called by {@link EnglishTokenizer#getTokenList(String)}. */
	protected void protectEmoticons(List<StringBooleanPair> tokens)
	{
		for (StringBooleanPair token : tokens)
		{
			if (T_EMOTICONS.contains(token.s))
				token.b = true;
		}
	}
	
	/** Called by {@link EnglishTokenizer#getTokenList(String)}. */
	protected void protectAbbreviations(List<StringBooleanPair> tokens)
	{
		String lower;
		
		for (StringBooleanPair token : tokens)
		{
			lower = token.s.toLowerCase();
			
			if (T_ABBREVIATIONS.contains(lower) || P_ABBREVIATION.matcher(lower).find())
				token.b = true;
		}
	}
	
	/** Called by {@link EnglishTokenizer#getTokenList(String)}. */
	protected void protectFilenames(List<StringBooleanPair> tokens)
	{
		String lower;
		
		for (StringBooleanPair token : tokens)
		{
			lower = token.s.toLowerCase();
			
			if (PTLink.FILE_EXTS.matcher(lower).find())
				token.b = true;
		}
	}
	
	protected void protectTwits(List<StringBooleanPair> tokens)
	{
		for (StringBooleanPair token : tokens)
		{
			char c = token.s.charAt(0);
			
			if ((c == '@' || c == '#') && MPLib.isAlnum(token.s.substring(1)))
				token.b = true;
		}
	}
	
	protected void replaceProtects(List<StringBooleanPair> tokens, Replacer rep)
	{
		for (StringBooleanPair token : tokens)
		{
			if (!token.b)
				token.s = rep.replace(token.s);
		}
	}
	
	protected void replaceHyphens(List<StringBooleanPair> tokens)
	{
		for (StringBooleanPair token : tokens)
		{
			if (!token.b && P_HYPHEN_LIST.matcher(token.s.toLowerCase()).find())
				token.s = P_HYPHEN.matcher(token.s).replaceAll(S_HYPHEN);
		}
	}
	
	protected void recoverPatterns(List<StringBooleanPair> tokens, Pattern p, String replacement)
	{
		for (StringBooleanPair token : tokens)
			token.s = p.matcher(token.s).replaceAll(replacement);
	}
	
	protected List<StringBooleanPair> tokenizeCompounds(List<StringBooleanPair> oTokens)
	{
		List<StringBooleanPair> nTokens = new ArrayList<StringBooleanPair>();
		int idx;
		
		for (StringBooleanPair oToken : oTokens)
		{
			if (oToken.b || (idx = M_COMPOUNDS.get(oToken.s.toLowerCase()) - 1) < 0)
				nTokens.add(oToken);
			else
			{
				for (IntIntPair p : L_COMPOUNDS.get(idx))
					nTokens.add(new StringBooleanPair(oToken.s.substring(p.i1, p.i2), true));
			}
		}
		
		return nTokens;
	}
	
	/** Called by {@link EnglishTokenizer#getTokenList(String)}. */
	protected List<StringBooleanPair> tokenizePatterns(List<StringBooleanPair> oTokens, Replacer rep)
	{
		List<StringBooleanPair> nTokens = new ArrayList<StringBooleanPair>();
		
		for (StringBooleanPair oToken : oTokens)
		{
			if (oToken.b)	nTokens.add(oToken);
			else			tokenizePatternsAux(nTokens, rep, oToken.s);
		}
		
		return nTokens;
	}
	
	/** Called by {@link EnglishTokenizer#tokenizePatterns(List, Replacer)}. */
	private void tokenizePatternsAux(List<StringBooleanPair> tokens, Replacer rep, String str)
	{
		for (String token : P_DELIM.split(rep.replace(str).trim()))
		{
			if (token.startsWith(S_PROTECTED))
				tokens.add(new StringBooleanPair(token.substring(N_PROTECTED), true));
			else if (!token.isEmpty())
				tokens.add(new StringBooleanPair(token, false));
		}
	}
	
	private class SubstitutionOne implements Substitution
	{
		@Override
		public void appendSubstitution(MatchResult match, TextBuffer dest)
		{
			dest.append(S_DELIM);
			dest.append(S_PROTECTED);
			dest.append(match.group(0));
			dest.append(S_DELIM);
		}
	}
	
	private class SubstitutionTwo implements Substitution
	{
		@Override
		public void appendSubstitution(MatchResult match, TextBuffer dest)
		{
			dest.append(match.group(1));
			dest.append(S_DELIM);
			dest.append(match.group(2));
		}
	}

	private class SubstitutionD0D implements Substitution
	{
		@Override
		public void appendSubstitution(MatchResult match, TextBuffer dest)
		{
			dest.append(match.group(1));
			dest.append(S_D0D+M_D0D.get(match.group(2))+"_");
			dest.append(match.group(3));
		}
	}
	
	private class SubstitutionOnePlus implements Substitution
	{
		@Override
		public void appendSubstitution(MatchResult match, TextBuffer dest)
		{
			dest.append(S_DELIM);
			dest.append(S_PROTECTED);
			dest.append(match.group(1));
			dest.append(S_DELIM);
			dest.append(match.group(3));
		}
	}
}
