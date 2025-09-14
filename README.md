# FQNFolder

FQNFolder is an IntelliJ IDEA plugin that automatically folds long **full-qualified names (FQNs)** in Java code. When class names, interface names, or static member references to exceed a configurable threshold, the plugin collapses them into concise forms to improve code readability.

## Features
1. Folds long names based on a configurable length threshold (default: 16 characters)
2. Adds minimal prefixes when multiple classes share the same simple name  
   *Example:*  
   `java.awt.List` and `java.util.List` → `awt.List` and `util.List`
3. Handles type references, generic parameters, and static member access  
   *Example:* `java.lang.System.out` → `System.out`
4. Settings changes automatically refresh all open editors without IDE restart


### Example

```java
// Original code
java.util.List<String> list = new java.util.ArrayList<>();

// Folded view
List<String> list = new ArrayList<>();

// Original code
java.awt.List listA = new java.awt.List();
java.util.List<String> listB = new ArrayList<>();

// Folded view
awt.List listA = new awt.List();
util.List<String> listB = new ArrayList<>();
```
