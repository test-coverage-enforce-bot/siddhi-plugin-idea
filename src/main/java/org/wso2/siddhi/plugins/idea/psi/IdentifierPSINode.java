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

package org.wso2.siddhi.plugins.idea.psi;

import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.antlr.jetbrains.adaptor.psi.ANTLRPsiLeafNode;
import org.antlr.jetbrains.adaptor.psi.IdentifierDefSubtree;
import org.antlr.jetbrains.adaptor.psi.Trees;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wso2.siddhi.plugins.idea.SiddhiLanguage;
import org.wso2.siddhi.plugins.idea.SiddhiTypes;
import org.wso2.siddhi.plugins.idea.psi.references.AttributeNameReference;
import org.wso2.siddhi.plugins.idea.psi.references.StreamIdReference;
import org.wso2.siddhi.plugins.idea.psi.references.TargetReference;

/**
 * ANTLRPsiNode which represents identifiers in siddhi file.
 */
public class IdentifierPSINode extends ANTLRPsiLeafNode implements PsiNamedElement, PsiNameIdentifierOwner {

    public IdentifierPSINode(IElementType type, CharSequence text) {
        super(type, text);
    }

    @Override
    public String getName() {
        return getText();
    }

    /**
     * Alter this node to have text specified by the argument. Do this by
     * creating a new node through parsing of an ID and then doing a
     * replace.
     */
    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        if (getParent() == null) {
            return this;
        }
        PsiElement newID = Trees.createLeafFromText(getProject(), SiddhiLanguage.INSTANCE, getContext(), name,
                SiddhiTypes.IDENTIFIER);
        if (newID != null) {
            // use replace on leaves but replaceChild on ID nodes that are part of defs/decls.
            return this.replace(newID);
        }
        return this;
    }

    /**
     * Create and return a PsiReference object associated with this ID
     * node. The reference object will be asked to resolve this ref
     * by using the text of this node to identify the appropriate definition
     * site. The definition site is typically a subtree for a function
     * or variable definition whereas this reference is just to this ID
     * leaf node.
     * <p>
     * As the AST factory has no context and cannot create different kinds
     * of PsiNamedElement nodes according to context, every ID node
     * in the tree will be of this type. So, we distinguish references
     * from definitions or other uses by looking at context in this method
     * as we have parent (context) information.
     */
    @Override
    public PsiReference getReference() {
        PsiElement parent = getParent();
        // Returning all the stream ids after the insert into clause in a query
        // In here we are  explicitly suggesting all the stream ids even though we have checked for the parent type
        // target
        PsiElement prevVisSibling = PsiTreeUtil.prevVisibleLeaf(this);
        if (prevVisSibling != null) {
            IElementType preVisibleSiblingType = ((LeafPsiElement) prevVisSibling).getElementType();
            // "INTO" keyword can be found in the "update or insert into" and in the normal "inset into" clause. In
            // here we need to give all stream ids only to normal "insert into" clause. So we check whether the
            // element is not a child of the parent type UpdateOrInsertInto node.
            if (PsiTreeUtil.getParentOfType(parent, TargetNode.class) != null && preVisibleSiblingType == SiddhiTypes
                    .INTO && PsiTreeUtil.getParentOfType(parent, UpdateOrInsertIntoNode.class) == null) {
                return new StreamIdReference(this);
            }
        }
        // Do not change the order of the if statements. This order is aligned with the psi tree hierarchy
        if (PsiTreeUtil.getParentOfType(parent, TargetNode.class) != null) {
            return new TargetReference(this);
        } else if (PsiTreeUtil.getParentOfType(parent, AttributeNameNode.class) != null && PsiTreeUtil.getParentOfType
                (parent, AttributeReferenceNode.class) != null) {
            return new AttributeNameReference(this);
        } else if (PsiTreeUtil.getParentOfType(parent, StreamIdNode.class) != null) {
            return new StreamIdReference(this);
        }
        return null;
    }

    @Override
    public ItemPresentation getPresentation() {
        PsiElement parent = getParent();
        if (parent instanceof IdentifierDefSubtree) {
            return ((IdentifierDefSubtree) parent).getPresentation();
        }
        return super.getPresentation();
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return null;
    }
}
