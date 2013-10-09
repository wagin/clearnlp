/**
* Copyright (c) 2009-2012, Regents of the University of Colorado
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the University of Colorado at Boulder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
* LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
* INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
* CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
* ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
* POSSIBILITY OF SUCH DAMAGE.
*/
package com.clearnlp.component.morph;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.clearnlp.morphology.MPTag;


/** @author Jinho D. Choi ({@code jdchoi77@gmail.com}) */
public class EnglishMPAnalyzerTest
{
	@Test
	public void testEnglishMPAnalyzer() throws Exception
	{
		String[][] tokens = {
				// abbreviation
				{"n't", "RB", "not"},
				{"na" , "TO", "to"},

				// ordinal
				{"1st"   , "XX", MPTag.LEMMA_ORDINAL},
				{"12nd"  , "XX", MPTag.LEMMA_ORDINAL},
				{"23rd"  , "XX", MPTag.LEMMA_ORDINAL},
				{"34th"  , "XX", MPTag.LEMMA_ORDINAL},
				{"first" , "XX", MPTag.LEMMA_ORDINAL},
				{"third" , "XX", MPTag.LEMMA_ORDINAL},
				{"fourth", "XX", MPTag.LEMMA_ORDINAL},
				
				// cardinal
				{"zero"    , "XX", MPTag.LEMMA_CARDINAL},
				{"ten"     , "XX", MPTag.LEMMA_CARDINAL},
				{"tens"    , "XX", MPTag.LEMMA_CARDINAL},
				{"eleven"  , "XX", MPTag.LEMMA_CARDINAL},
				{"fourteen", "XX", MPTag.LEMMA_CARDINAL},
				{"thirties", "XX", MPTag.LEMMA_CARDINAL},
				
				// verb: 3rd-person singular
				{"studies", "VBZ", "study"},
				{"pushes" , "VBZ", "push"},
				{"takes"  , "VBZ", "take"},
				
				// verb: gerund
				{"lying"  , "VBG", "lie"},
				{"feeling", "VBG", "feel"},
				{"running", "VBG", "run"},
				{"taking" , "VBG", "take"},

				// verb: past (participle)
				{"denied" , "VBD", "deny"},
				{"entered", "VBD", "enter"},
				{"zipped" , "VBD", "zip"},
				{"heard"  , "VBD", "hear"},
				{"drawn"  , "VBN", "draw"},
				{"clung"  , "VBN", "cling"},

				// verb: irregular
				{"chivvies" , "VBZ", "chivy"},
				{"took"     , "VBD", "take"},
				{"beaten"   , "VBN", "beat"},
				{"forbidden", "VBN", "forbid"},
				{"bitten"   , "VBN", "bite"},
				{"spoken"   , "VBN", "speak"},
				{"woven"    , "VBN", "weave"},
				{"woken"    , "VBN", "wake"},
				{"slept"    , "VBD", "sleep"},
				{"fed"      , "VBD", "feed"},
				{"led"      , "VBD", "lead"},
				{"learnt"   , "VBD", "learn"},
				{"rode"     , "VBD", "ride"},
				{"spoke"    , "VBD", "speak"},
				{"woke"     , "VBD", "wake"},
				{"wrote"    , "VBD", "write"},
				{"bore"     , "VBD", "bear"},
				{"stove"    , "VBD", "stave"},
				{"drove"    , "VBD", "drive"},
				{"wove"     , "VBD", "weave"},
				
				// noun: plural
				{"studies"  , "NNS", "study"},
				{"crosses"  , "NNS", "cross"},
				{"areas"    , "NNS", "area"},
				{"gentlemen", "NNS", "gentleman"},
				{"vertebrae", "NNS", "vertebra"},
				{"foci"     , "NNS", "focus"},

				// noun: irregular
				{"indices"   , "NNS", "index"},
				{"appendices", "NNS", "appendix"},
				{"wolves"    , "NNS", "wolf"},
				{"knives"    , "NNS", "knife"},
				{"quizzes"   , "NNS", "quiz"},
				{"mice"      , "NNS", "mouse"},
				{"geese"     , "NNS", "goose"},
				{"teeth"     , "NNS", "tooth"},
				{"feet"      , "NNS", "foot"},
				{"analyses"  , "NNS", "analysis"},
				{"optima"    , "NNS", "optimum"},
				{"lexica"    , "NNS", "lexicon"},
				{"corpora"   , "NNS", "corpus"},

				// adjective: comparative
				{"easier" , "JJR", "easy"},
				{"smaller", "JJR", "small"},
				{"bigger" , "JJR", "big"},
				{"larger" , "JJR", "large"},

				// adjective: superative
				{"easiest" , "JJS", "easy"},
				{"smallest", "JJS", "small"},
				{"biggest" , "JJS", "big"},
				{"largest" , "JJS", "large"},
				
				// adjective: irregular
				{"best", "JJS", "good"},
				
				// adverb: comparative
				{"earlier", "RBR", "early"},
				{"sooner" , "RBR", "soon"},
				{"larger" , "RBR", "large"},
				
				{"earliest", "RBS", "early"},
				{"soonest" , "RBS", "soon"},
				{"largest" , "RBS", "large"},
				
				// adverb: irregular
				{"best", "RBS", "well"},
				
				// URL
				{"http://www.google.com"     , "XX", "#url#"},
				{"www.google.com"            , "XX", "#url#"},
				{"mailto:somebody@google.com", "XX", "#url#"},
				{"some-body@google+.com"     , "XX", "#url#"},
				
				// numbers
				{"10%", "XX", "0"},
				{"$10", "XX", "0"},
				{".01", "XX", "0"},
				{"12.34", "XX", "0"},
				{"12,34,56", "XX", "0"},
				{"12-34-56", "XX", "0"},
				{"12/34/46", "XX", "0"},
				{"A.01", "XX", "a.0"},
				{"A:01", "XX", "a:0"},
				{"A/01", "XX", "a/0"},
				{"$10.23,45:67-89/10%", "XX", "0"},
				
				// punctuation
				{".!?-*=~,", "XX", ".!?-*=~,"},
				{"..!!??--**==~~,,", "XX", "..!!??--**==~~,,"},
				{"...!!!???---***===~~~,,,", "XX", "..!!??--**==~~,,"},
				{"....!!!!????----****====~~~~,,,,", "XX", "..!!??--**==~~,,"}};
		
		EnglishMPAnalyzer analyzer = new EnglishMPAnalyzer();
		String lemma;
		
		for (String[] token : tokens)
		{
			lemma = analyzer.getLemma(token[0], token[1]);
			assertEquals(token[2], lemma);
		}
	}
}