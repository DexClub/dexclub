package io.github.dexclub.node

import io.github.dexclub.core.workspace.ClassVisualKind

sealed class ClassTreeNode {
    data class ClassTreeClassItem(
        val className: String,
        val displayName: String,
        val classVisualKind: ClassVisualKind,
    )

    data class PackageNode(
        val name: String,
        val fullPath: String,
        val children: MutableList<ClassTreeNode>,
    ) : ClassTreeNode()

    data class ClassNode(
        val className: String,
        val displayName: String,
        val classVisualKind: ClassVisualKind,
    ) : ClassTreeNode()

    override fun toString(): String {
        val sb = StringBuilder()
        renderToString(sb, "", true, isRoot = true)
        return sb.toString()
    }

    private fun renderToString(
        sb: StringBuilder,
        prefix: String,
        isLast: Boolean,
        isRoot: Boolean
    ) {
        // 1. 确定当前行的符号
        if (!isRoot) {
            sb.append(prefix)
            sb.append(if (isLast) "└── " else "├── ")
        }

        // 2. 打印节点名称
        when (this) {
            is PackageNode -> {
                sb.append(name).append("/\n")

                // 3. 递归处理子节点
                val nextPrefix = if (isRoot) "" else prefix + (if (isLast) "    " else "│   ")
                children.forEachIndexed { index, child ->
                    child.renderToString(
                        sb,
                        nextPrefix,
                        index == children.size - 1,
                        isRoot = false
                    )
                }
            }

            is ClassNode -> {
                sb.append(displayName).append("\n")
            }
        }
    }

    companion object {
        fun parse(items: List<ClassTreeClassItem>): ClassTreeNode {
            // 创建一个虚拟根节点，用于承载所有顶级包
            val root = PackageNode(name = "root", fullPath = "", children = mutableListOf())

            for (item in items) {
                // 1. 处理路径分隔符（支持 . 或 /）
                val parts = item.className.replace('/', '.').split('.')

                // 2. 获取类名和包路径部分
                val packageParts = parts.dropLast(1)

                var currentNode = root

                // 3. 逐级查找或创建包节点
                val currentPathParts = mutableListOf<String>()
                for (part in packageParts) {
                    currentPathParts.add(part)
                    val fullPath = currentPathParts.joinToString(".")

                    // 在当前节点的子节点中查找是否已存在该包
                    var packageNode = currentNode.children
                        .filterIsInstance<PackageNode>()
                        .find { it.name == part }

                    if (packageNode == null) {
                        packageNode = PackageNode(
                            name = part,
                            fullPath = fullPath,
                            children = mutableListOf()
                        )
                        currentNode.children.add(packageNode)
                    }
                    currentNode = packageNode
                }

                // 4. 将类记录封装为 ClassNode 放入最终的包节点中
                currentNode.children.add(
                    ClassNode(
                        className = item.className,
                        displayName = item.displayName,
                        classVisualKind = item.classVisualKind,
                    )
                )
            }

            // 整理：对每一层进行排序（可选：包在前，类在后；或者按字母排序）
            sortTree(root)

            return root
        }

        private fun sortTree(node: ClassTreeNode) {
            if (node is PackageNode) {
                // 排序逻辑：PackageNode 在前，ClassNode 在后，内部按名称排序
                node.children.sortWith(classTreeNodeComparator)
                // 递归排序子节点
                node.children.forEach { sortTree(it) }
            }
        }

        private val classTreeNodeComparator = Comparator<ClassTreeNode> { left, right ->
            val leftKind = if (left is PackageNode) 0 else 1
            val rightKind = if (right is PackageNode) 0 else 1
            if (leftKind != rightKind) {
                return@Comparator leftKind.compareTo(rightKind)
            }

            compareAndroidStudioNames(left.sortDisplayName(), right.sortDisplayName())
        }

        private fun ClassTreeNode.sortDisplayName(): String {
            return when (this) {
                is PackageNode -> name
                is ClassNode -> displayName
            }
        }

        private fun compareAndroidStudioNames(
            left: String,
            right: String,
        ): Int {
            var leftIndex = 0
            var rightIndex = 0
            while (leftIndex < left.length && rightIndex < right.length) {
                val leftChar = left[leftIndex]
                val rightChar = right[rightIndex]
                if (leftChar.isDigit() && rightChar.isDigit()) {
                    val digitCompare = compareNumberRun(
                        left = left,
                        leftStart = leftIndex,
                        right = right,
                        rightStart = rightIndex,
                    )
                    if (digitCompare.result != 0) return digitCompare.result
                    leftIndex = digitCompare.leftEnd
                    rightIndex = digitCompare.rightEnd
                    continue
                }

                val leftNormalized = leftChar.normalizeForAndroidStudioSort()
                val rightNormalized = rightChar.normalizeForAndroidStudioSort()
                val charCompare = leftNormalized.compareTo(rightNormalized)
                if (charCompare != 0) return charCompare

                val exactCompare = leftChar.compareTo(rightChar)
                if (exactCompare != 0) return exactCompare

                leftIndex += 1
                rightIndex += 1
            }

            return (left.length - leftIndex).compareTo(right.length - rightIndex)
        }

        private fun Char.normalizeForAndroidStudioSort(): Char {
            return when (this) {
                '$' -> Char.MAX_VALUE
                else -> lowercaseChar()
            }
        }

        private fun compareNumberRun(
            left: String,
            leftStart: Int,
            right: String,
            rightStart: Int,
        ): NumberRunCompare {
            val leftEnd = left.findNumberRunEnd(leftStart)
            val rightEnd = right.findNumberRunEnd(rightStart)
            val leftTrimmedStart = left.findFirstNonZero(leftStart, leftEnd)
            val rightTrimmedStart = right.findFirstNonZero(rightStart, rightEnd)
            val leftTrimmedLength = leftEnd - leftTrimmedStart
            val rightTrimmedLength = rightEnd - rightTrimmedStart
            if (leftTrimmedLength != rightTrimmedLength) {
                return NumberRunCompare(
                    result = leftTrimmedLength.compareTo(rightTrimmedLength),
                    leftEnd = leftEnd,
                    rightEnd = rightEnd,
                )
            }

            for (index in 0 until leftTrimmedLength) {
                val compare = left[leftTrimmedStart + index].compareTo(right[rightTrimmedStart + index])
                if (compare != 0) {
                    return NumberRunCompare(
                        result = compare,
                        leftEnd = leftEnd,
                        rightEnd = rightEnd,
                    )
                }
            }

            return NumberRunCompare(
                result = (leftEnd - leftStart).compareTo(rightEnd - rightStart),
                leftEnd = leftEnd,
                rightEnd = rightEnd,
            )
        }

        private fun String.findNumberRunEnd(start: Int): Int {
            var index = start
            while (index < length && this[index].isDigit()) {
                index += 1
            }
            return index
        }

        private fun String.findFirstNonZero(
            start: Int,
            end: Int,
        ): Int {
            var index = start
            while (index < end - 1 && this[index] == '0') {
                index += 1
            }
            return index
        }

        private data class NumberRunCompare(
            val result: Int,
            val leftEnd: Int,
            val rightEnd: Int,
        )
    }
}

data class FlattenedNode(
    val node: ClassTreeNode,
    val depth: Int
)

fun ClassTreeNode.flatten(
    depth: Int = 0,
    expandedPaths: Set<String>
): List<FlattenedNode> {
    val result = mutableListOf<FlattenedNode>()

    if (this is ClassTreeNode.PackageNode) {
        if (name == "root") {
            // 根节点不展示，只递归子节点
            children.forEach { result.addAll(it.flatten(depth, expandedPaths)) }
        } else {
            // 1. 添加当前节点
            result.add(FlattenedNode(this, depth))

            // 2. 如果展开了，递归子节点
            if (this.fullPath in expandedPaths) {
                children.forEach { child ->
                    result.addAll(child.flatten(depth + 1, expandedPaths))
                }
            }
        }
    } else if (this is ClassTreeNode.ClassNode) {
        result.add(FlattenedNode(this, depth))
    }

    return result
}
