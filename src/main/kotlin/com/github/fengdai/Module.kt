package com.github.fengdai

class Module(
        val packageName: String
) {

    val rClasses = mutableListOf<RClass>()
    val parents = mutableListOf<Module>()
    val children = mutableListOf<Module>()
    val size = 0
        get() {
            if (field == 0) {
                field = rClasses.fold(0) { size, it -> size + it.fieldNames.size }
            }
            return field
        }
    private val sameModules = mutableListOf<Module>()

    fun addRClass(rClass: RClass) {
        rClasses.add(0, rClass)
    }

    fun maybeAsSameModule(module: Module): Boolean {
        if (hasSameFieldsWith(module)) {
            if (!sameModules.contains(module)) sameModules.add(module)
            return true
        }
        return false
    }

    fun hasSameFieldsWith(module: Module): Boolean {
        val copy = rClasses.toMutableList()
        for (r in module.rClasses) {
            val myR = copy.find { it.kind == r.kind }
            if (myR == null || myR.fieldNames != r.fieldNames) {
                return false
            }
            copy.remove(myR)
        }
        return copy.isEmpty()
    }

    fun dependsOn(module: Module): Boolean {
        val copy = rClasses.toMutableList()
        for (r in module.rClasses) {
            val myR = copy.find { it.kind == r.kind }
            if (myR == null || !myR.fieldNames.containsAll(r.fieldNames)) {
                return false
            }
            copy.remove(myR)
        }
        return true
    }

    override fun hashCode(): Int {
        return packageName.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Module) return false
        return packageName == other.packageName
    }

    override fun toString(): String {
        return packageName
    }

    fun stringForRender(): String {
        val names = sameModules.map { it.packageName }
                .toList()
                .fold(packageName) { string, it -> string + "\n$it" }.toString()
        return "$names [$size]"
    }
}
