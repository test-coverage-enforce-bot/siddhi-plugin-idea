/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.siddhi.plugins.idea.completion.executionelements.query;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.wso2.siddhi.plugins.idea.SiddhiTypes;
import org.wso2.siddhi.plugins.idea.psi.AnonymousStreamNode;
import org.wso2.siddhi.plugins.idea.psi.OutputEventTypeNode;
import org.wso2.siddhi.plugins.idea.psi.OutputRateNode;
import org.wso2.siddhi.plugins.idea.psi.QueryInputNode;
import org.wso2.siddhi.plugins.idea.psi.QuerySectionNode;

import static org.wso2.siddhi.plugins.idea.completion.SiddhiCompletionUtils.addBeginingOfQueryOutputKeywords;
import static org.wso2.siddhi.plugins.idea.completion.SiddhiCompletionUtils.addIntoKeyword;
import static org.wso2.siddhi.plugins.idea.completion.SiddhiCompletionUtils.addOutputEventTypeKeywords;
import static org.wso2.siddhi.plugins.idea.completion.SiddhiCompletionUtils.addSuggestionsAfterQueryInput;
import static org.wso2.siddhi.plugins.idea.completion.executionelements.query.QueryInputCompletionContributor.queryInputCompletion;
import static org.wso2.siddhi.plugins.idea.completion.executionelements.query.QueryOutputCompletionContributor.queryOutputCompletion;
import static org.wso2.siddhi.plugins.idea.completion.executionelements.query.QueryOutputRateCompletionContributor.queryOutputRateCompletion;
import static org.wso2.siddhi.plugins.idea.completion.executionelements.query.QuerySectionCompletionContributor.querySectionCompletion;
import static org.wso2.siddhi.plugins.idea.completion.util.KeywordCompletionUtils.getPreviousVisibleSiblingSkippingComments;

/**
 * Provides code completions for queries.
 */
public class QueryCompletionContributor {

    public static void queryCompletion(@NotNull CompletionResultSet result, PsiElement element,
                                       PsiElement prevVisibleSibling,
                                       IElementType prevVisibleSiblingElementType,
                                       PsiElement prevPreVisibleSibling) {

        // Suggestions related to QueryInputNode
        if (PsiTreeUtil.getParentOfType(element, QueryInputNode.class) != null) {
            queryInputCompletion(result, element, prevVisibleSibling, prevVisibleSiblingElementType,
                    prevPreVisibleSibling);
            return;
        }
        /*
        * Suggestions after a anonymous stream.
        * Had to define here because in this situation the current element will not be in the QueryInputNode.
        * It will be under QueryNode as a PsiError element.(This means still antlr can't match to a rule)
        * So we have to give code suggestion after the QueryInputNode.
        * */
        if (PsiTreeUtil.getParentOfType(prevVisibleSibling, QueryInputNode.class) != null
                && PsiTreeUtil.getParentOfType(prevVisibleSibling, AnonymousStreamNode.class) != null) {
            if (PsiTreeUtil.getParentOfType(prevVisibleSibling, OutputEventTypeNode.class) != null
                    || (prevVisibleSiblingElementType == SiddhiTypes.CLOSE_PAR
                    && PsiTreeUtil.getParentOfType(prevPreVisibleSibling, OutputEventTypeNode.class) != null)) {
                addSuggestionsAfterQueryInput(result);
                return;
            }
        }
        // Suggestions related to QuerySectionNode
        if (PsiTreeUtil.getParentOfType(element, QuerySectionNode.class) != null) {
            querySectionCompletion(result, element, prevVisibleSibling, prevVisibleSiblingElementType,
                    prevPreVisibleSibling);
            return;
        }
        IElementType prevPreVisibleSiblingElementType = ((LeafPsiElement) prevPreVisibleSibling).getElementType();
        // Suggestions related to outputRateNode
        if (PsiTreeUtil.getParentOfType(element, OutputRateNode.class) != null) {
            queryOutputRateCompletion(result, element, prevVisibleSibling, prevVisibleSiblingElementType,
                    prevPreVisibleSibling, prevPreVisibleSiblingElementType);
            return;
        }
        // suggesting keywords in the beginning of a query_output rule
        // This provides suggestions after ->OUTPUT output_rate_type? EVERY INT_LITERAL EVENTS in output_rate rule
        if (PsiTreeUtil.getParentOfType(prevVisibleSibling, OutputRateNode.class) != null
                && prevVisibleSiblingElementType == SiddhiTypes.EVENTS
                && prevPreVisibleSiblingElementType == SiddhiTypes.INT_LITERAL) {
            addBeginingOfQueryOutputKeywords(result);
            return;
        }
        // Suggestions related to QueryOutputNode

        // suggestions after INSERT keyword
        if (prevVisibleSiblingElementType == SiddhiTypes.INSERT && (PsiTreeUtil.getParentOfType
                (prevPreVisibleSibling, OutputRateNode.class) != null || PsiTreeUtil.getParentOfType
                (prevPreVisibleSibling, QuerySectionNode.class) != null || PsiTreeUtil.getParentOfType
                (prevPreVisibleSibling, QueryInputNode.class) != null)) {
            addOutputEventTypeKeywords(result);
            addIntoKeyword(result);
            return;
        }
        // suggesting INTO keyword after a output event type in a query
        PsiElement parentOfPrevVisSibling = prevVisibleSibling.getParent();
        if (parentOfPrevVisSibling instanceof OutputEventTypeNode) {
            PsiElement prevVisibleSiblingOfParent = getPreviousVisibleSiblingSkippingComments(parentOfPrevVisSibling);
            IElementType elementTypeOfPrevVisibleSiblingOfParent = null;
            if (prevVisibleSiblingOfParent != null) {
                elementTypeOfPrevVisibleSiblingOfParent = ((LeafPsiElement) prevVisibleSiblingOfParent)
                        .getElementType();
            }
            if (elementTypeOfPrevVisibleSiblingOfParent == SiddhiTypes.INSERT) {
                addIntoKeyword(result);
                return;
            }
        }
        // Suggestions inside a QueryOutputNode
        queryOutputCompletion(result, element, prevVisibleSibling, prevVisibleSiblingElementType,
                prevPreVisibleSibling);
    }
}
