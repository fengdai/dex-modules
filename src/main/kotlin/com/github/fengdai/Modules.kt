package com.github.fengdai

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object Modules {

    @JvmStatic fun main(vararg args: String) {
        args.map { FileInputStream(it).use { it.readBytes() } }
                .toList()
                .let { module(it).let { genGraphvizFile(it) } }
    }

    @JvmStatic fun module(vararg files: File): Module = module(files.map { it.readBytes() })

    @JvmStatic fun module(bytes: List<ByteArray>): Module {
        RClasses.list(bytes).let { return toModule(it) }
    }

    @JvmStatic private fun toModule(rClasses: List<RClass>): Module {
        val map = rClasses.fold(mutableMapOf<String, Module>()) { map, it ->
            val packageName = it.typeName.substring(0, it.typeName.lastIndexOf("."))
            if (!map.contains(packageName)) {
                val module = Module(packageName)
                module.addRClass(it)
                map.put(packageName, module)
            } else {
                map[packageName]!!.addRClass(it)
            }
            map
        }
        val modules = map.values.sortedByDescending { it.size }
        val root = modules[0]
        for (i in 1..modules.size - 1) {
            val child = modules[i]
            val parents = mutableListOf<Module>()
            findAllNodes(root, child, parents)
            for (parent in parents) {
                if (!child.parents.contains(parent)) child.parents.add(parent)
                if (!parent.children.contains(child)) parent.children.add(child)
            }
        }
        return root
    }

    internal fun genGraphvizFile(root: Module) {
        val txt = renderGraphviz(root)
        FileOutputStream(".${File.separator}${root.packageName}.gv").use { it.write(txt.toByteArray()) }
    }

    internal fun renderGraphviz(root: Module): String {
        StringBuilder().let {
            it.appendln("digraph G {")
            it.appendln("rankdir=BT")
            it.appendln("node [shape=record]")
            render(it, root)
            it.appendln("}")
            return it.toString()
        }
    }

    internal fun render(builder: StringBuilder,
                        parent: Module,
                        rendered: MutableList<Pair<Module, Module>> = mutableListOf()) {
        for (child in parent.children) {
            val relation = Pair(parent, child)
            if (!rendered.contains(relation)) {
                builder.appendln("\"${parent.stringForRender()}\" -> \"${child.stringForRender()}\"")
                rendered += relation
            }
            render(builder, child, rendered)
        }
    }

    private fun findAllNodes(parent: Module, module: Module, parents: MutableList<Module>) {
        if (parent.maybeAsSameModule(module)) {
            return
        }
        if (isParent(parent, module)) {
            parents.add(parent)

        } else {
            for (child in parent.children) {
                if (child.maybeAsSameModule(module)) {
                    return
                }
                findAllNodes(child, module, parents)
            }
        }
    }

    private fun isParent(parent: Module, module: Module): Boolean {
        if (parent.dependsOn(module)) {
            for (child in parent.children) {
                if (child.dependsOn(module)) {
                    return false
                }
            }
            return true
        }
        return false
    }
}
