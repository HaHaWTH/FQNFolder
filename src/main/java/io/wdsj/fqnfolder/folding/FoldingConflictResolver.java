package io.wdsj.fqnfolder.folding;

import io.wdsj.fqnfolder.folding.QualifiedNameFoldingBuilder.QualifiedReference;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FoldingConflictResolver {

    public Map<QualifiedReference, String> resolveConflicts(
            Map<String, List<QualifiedReference>> qualifiedNames) {

        Map<QualifiedReference, String> result = new HashMap<>();

        for (Map.Entry<String, List<QualifiedReference>> entry : qualifiedNames.entrySet()) {
            String simpleName = entry.getKey();
            List<QualifiedReference> references = entry.getValue();

            if (references.size() == 1) {
                result.put(references.getFirst(), simpleName);
            } else {
                resolveConflictGroup(simpleName, references, result);
            }
        }

        return result;
    }

    private void resolveConflictGroup(String simpleName, List<QualifiedReference> references,
                                      Map<QualifiedReference, String> result) {

        Map<String, List<QualifiedReference>> groupedByQualified = new HashMap<>();
        for (QualifiedReference ref : references) {
            groupedByQualified.computeIfAbsent(ref.qualifiedName(), k -> new ArrayList<>()).add(ref);
        }

        if (groupedByQualified.size() == 1) {
            for (QualifiedReference ref : references) {
                result.put(ref, simpleName);
            }
            return;
        }

        Map<String, String> qualifiedToFolded = findMinimumDistinguishingNames(
                groupedByQualified.keySet()
        );

        for (Map.Entry<String, List<QualifiedReference>> entry : groupedByQualified.entrySet()) {
            String qualifiedName = entry.getKey();
            String foldedName = qualifiedToFolded.get(qualifiedName);

            for (QualifiedReference ref : entry.getValue()) {
                result.put(ref, foldedName);
            }
        }
    }

    private Map<String, String> findMinimumDistinguishingNames(Set<String> qualifiedNames) {
        Map<String, String> result = new ConcurrentHashMap<>();
        List<String> namesList = new ArrayList<>(qualifiedNames);

        namesList.parallelStream().forEach(name -> {
            String[] parts = name.split("\\.");
            String simpleName = parts[parts.length - 1];

            boolean needsPrefix = false;
            for (String otherName : namesList) {
                if (!name.equals(otherName) && otherName.endsWith("." + simpleName)) {
                    needsPrefix = true;
                    break;
                }
            }

            if (!needsPrefix) {
                result.put(name, simpleName);
            } else {
                String distinguishedName = simpleName;
                for (int prefixLen = 1; prefixLen < parts.length; prefixLen++) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = parts.length - prefixLen - 1; i < parts.length; i++) {
                        if (!sb.isEmpty()) sb.append(".");
                        sb.append(parts[i]);
                    }
                    distinguishedName = sb.toString();

                    boolean isUnique = true;
                    for (String otherName : namesList) {
                        if (!name.equals(otherName) && otherName.endsWith(distinguishedName)) {
                            isUnique = false;
                            break;
                        }
                    }

                    if (isUnique) {
                        break;
                    }
                }
                result.put(name, distinguishedName);
            }
        });

        return result;
    }
}