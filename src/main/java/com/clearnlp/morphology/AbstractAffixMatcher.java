/**
 * Copyright (c) 2009/09-2012/08, Regents of the University of Colorado
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * Copyright 2012/09-2013/04, University of Massachusetts Amherst
 * Copyright 2013/05-Present, IPSoft Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.clearnlp.morphology;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.clearnlp.util.pair.Pair;
import com.google.common.collect.Lists;


/**
 * @since 2.0.0
 * @author Jinho D. Choi ({@code jdchoi77@gmail.com})
 */
abstract public class AbstractAffixMatcher
{
	protected String                      s_affixCanonicalForm;
	protected String                      s_affixPOS;
	protected Pattern                     p_originalPOS;
	protected List<AbstractAffixReplacer> l_replacers;
	
	public AbstractAffixMatcher(String affixCanonicalForm, String affixPOS, Pattern originalPOS)
	{
		init(affixCanonicalForm, affixPOS, originalPOS);
	}
	
	private void init(String affixCanonicalForm, String affixPOS, Pattern originalPOS)
	{
		s_affixCanonicalForm = affixCanonicalForm;
		s_affixPOS           = affixPOS;
		p_originalPOS        = originalPOS;
		l_replacers          = Lists.newArrayList();
	}
	
	public boolean matchesOriginalPOS(String pos)
	{
		return p_originalPOS == null || p_originalPOS.matcher(pos).find();
	}
	
	public void addReplacer(AbstractAffixReplacer replacer)
	{
		l_replacers.add(replacer);
	}
	
	protected Morpheme getAffixMorpheme()
	{
		return new Morpheme(s_affixCanonicalForm, s_affixPOS);
	}
	
	/** 
	 * Returns (BaseMorpheme, SuffixMorphem) if exists; otherwise, {@code null}.
	 * @param form the word-form in lower-case.
	 */
	abstract public Pair<Morpheme,Morpheme> getMorphemes(Map<String,Set<String>> baseMap, String form);
	
	/** 
	 * Returns (BaseMorpheme, SuffixMorphem) if exists; otherwise, {@code null}.
	 * @param form the word-form in lower-case.
	 */
	abstract public Pair<Morpheme,Morpheme> getMorphemes(Set<String> baseSet, String form);

	/** 
	 * Returns (BaseMorpheme, SuffixMorphem) if exists; otherwise, {@code null}.
	 * @param form the word-form in lower-case.
	 */
	abstract public Pair<Morpheme,Morpheme> getMorphemes(Set<String> baseSet, String form, String pos);
}