package io.wdsj.fqnfolder.folding;

import io.wdsj.fqnfolder.settings.PluginSettings;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class QualifiedNameFoldingBuilder extends FoldingBuilderEx {

    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement root,
                                                @NotNull Document document,
                                                boolean quick) {
        if (!(root instanceof PsiJavaFile)) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }

        PluginSettings settings = PluginSettings.getInstance();
        if (!settings.isEnabled()) {
            return FoldingDescriptor.EMPTY_ARRAY;
        }

        List<FoldingDescriptor> descriptors = new ArrayList<>();
        Map<String, List<QualifiedReference>> qualifiedNames = new HashMap<>();

        collectQualifiedReferences(root, qualifiedNames, settings.getFoldingThreshold());

        FoldingConflictResolver resolver = new FoldingConflictResolver();
        Map<QualifiedReference, String> foldedNames = resolver.resolveConflicts(qualifiedNames);

        for (Map.Entry<QualifiedReference, String> entry : foldedNames.entrySet()) {
            QualifiedReference ref = entry.getKey();
            String foldedName = entry.getValue();

            FoldingGroup group = FoldingGroup.newGroup("qualified-name");
            descriptors.add(new FoldingDescriptor(ref.node, ref.range, group) {
                @Nullable
                @Override
                public String getPlaceholderText() {
                    return foldedName;
                }
            });
        }

        return descriptors.toArray(FoldingDescriptor.EMPTY_ARRAY);
    }

    private void collectQualifiedReferences(PsiElement element,
                                            Map<String, List<QualifiedReference>> qualifiedNames,
                                            int threshold) {
        switch (element) {
            case PsiTypeElement typeElement -> {
                processTypeElement(typeElement, qualifiedNames, threshold);
            }
            case PsiNewExpression newExpression -> {
                processNewExpression(newExpression, qualifiedNames, threshold);
            }
            case PsiTypeCastExpression typeCast -> {
                PsiTypeElement castType = typeCast.getCastType();
                if (castType != null) {
                    processTypeElement(castType, qualifiedNames, threshold);
                }
            }
            case PsiInstanceOfExpression instanceOf -> {
                PsiTypeElement checkType = instanceOf.getCheckType();
                if (checkType != null) {
                    processTypeElement(checkType, qualifiedNames, threshold);
                }
            }
            case PsiReferenceExpression refExpr -> {
                processReferenceExpression(refExpr, qualifiedNames, threshold);
            }
            default -> {
            }
        }

        for (PsiElement child : element.getChildren()) {
            collectQualifiedReferences(child, qualifiedNames, threshold);
        }
    }

    private void processReferenceExpression(PsiReferenceExpression refExpr,
                                            Map<String, List<QualifiedReference>> qualifiedNames,
                                            int threshold) {
        String fullText = refExpr.getText();

        if (!fullText.contains(".")) {
            return;
        }

        PsiExpression qualifier = refExpr.getQualifierExpression();
        if (qualifier == null) {
            return;
        }

        if (!qualifier.textContains('.')) return;

        String qualifierText = qualifier.getText();

        if (!isClassReference(qualifier)) {
            return;
        }

        if (qualifierText.length() <= threshold) {
            return;
        }

        if (isInnerClassReference(qualifierText)) {
            return;
        }

        PsiElement resolved = refExpr.resolve();
        if (resolved instanceof PsiField || resolved instanceof PsiMethod) {
            PsiModifierListOwner modifierListOwner = (PsiModifierListOwner) resolved;

            if (modifierListOwner.hasModifierProperty(PsiModifier.STATIC)) {
                String simpleName = getSimpleName(qualifierText);

                TextRange qualifierRange = qualifier.getTextRange();

                QualifiedReference qRef = new QualifiedReference(
                        qualifier.getNode(),
                        qualifierRange,
                        qualifierText
                );
                qualifiedNames.computeIfAbsent(simpleName, k -> new ArrayList<>()).add(qRef);
            }
        }
    }

    private boolean isClassReference(PsiExpression expr) {
        if (!(expr instanceof PsiReferenceExpression refExpr)) {
            return false;
        }

        PsiElement resolved = refExpr.resolve();

        if (resolved instanceof PsiClass) {
            return true;
        }

        String text = expr.getText();
        if (text.contains(".")) {
            String[] parts = text.split("\\.");
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                return !lastPart.isEmpty() && Character.isUpperCase(lastPart.charAt(0));
            }
        }

        return false;
    }

    private void processTypeElement(PsiTypeElement typeElement,
                                    Map<String, List<QualifiedReference>> qualifiedNames,
                                    int threshold) {
        String typeText = typeElement.getText();

        int genericStart = typeText.indexOf('<');
        String mainType = genericStart > 0 ? typeText.substring(0, genericStart).trim() : typeText;

        if (mainType.contains(".") && mainType.length() > threshold && !isArrayType(mainType)) {
            if (!isInnerClassReference(mainType)) {
                String simpleName = getSimpleName(mainType);

                TextRange typeRange = typeElement.getTextRange();
                TextRange adjustedRange = genericStart > 0
                        ? new TextRange(typeRange.getStartOffset(), typeRange.getStartOffset() + genericStart)
                        : typeRange;

                QualifiedReference qRef = new QualifiedReference(
                        typeElement.getNode(),
                        adjustedRange,
                        mainType
                );
                qualifiedNames.computeIfAbsent(simpleName, k -> new ArrayList<>()).add(qRef);
            }
        }
    }

    private void processNewExpression(PsiNewExpression newExpression,
                                      Map<String, List<QualifiedReference>> qualifiedNames,
                                      int threshold) {
        PsiJavaCodeReferenceElement classReference = newExpression.getClassReference();
        if (classReference != null) {
            String text = classReference.getText();

            int genericStart = text.indexOf('<');
            String mainType = genericStart > 0 ? text.substring(0, genericStart).trim() : text;

            if (mainType.contains(".") && mainType.length() > threshold && !isInnerClassReference(mainType)) {
                String simpleName = getSimpleName(mainType);

                TextRange refRange = classReference.getTextRange();
                TextRange adjustedRange = genericStart > 0
                        ? new TextRange(refRange.getStartOffset(), refRange.getStartOffset() + genericStart)
                        : refRange;

                QualifiedReference qRef = new QualifiedReference(
                        classReference.getNode(),
                        adjustedRange,
                        mainType
                );
                qualifiedNames.computeIfAbsent(simpleName, k -> new ArrayList<>()).add(qRef);
            }
        }
    }

    private boolean isArrayType(String type) {
        return type.contains("[") || type.contains("]");
    }

    private boolean isInnerClassReference(String qualifiedName) {
        if (qualifiedName.contains("$")) {
            return true;
        }

        String[] parts = qualifiedName.split("\\.");
        if (parts.length >= 2) {
            String lastPart = parts[parts.length - 1];
            String secondLastPart = parts[parts.length - 2];

            if (!secondLastPart.isEmpty() && Character.isUpperCase(secondLastPart.charAt(0)) &&
                    !lastPart.isEmpty() && Character.isUpperCase(lastPart.charAt(0))) {
                return !isCommonPackageName(secondLastPart);
            }
        }

        return false;
    }

    private boolean isCommonPackageName(String name) {
        return name.equals(name.toLowerCase()) ||
                name.equals("awt") || name.equals("util") || name.equals("io") ||
                name.equals("nio") || name.equals("sql") || name.equals("net") ||
                name.equals("lang") || name.equals("math") || name.equals("text");
    }

    private String getSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        return null;
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return true;
    }

    public static class QualifiedReference {
        final ASTNode node;
        final TextRange range;
        final String qualifiedName;

        public QualifiedReference(ASTNode node, TextRange range, String qualifiedName) {
            this.node = node;
            this.range = range;
            this.qualifiedName = qualifiedName;
        }
    }
}