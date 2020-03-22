package tw.darkk6.adbremote.util

class ArgumentParser(vararg args: String) {

    private val paramMap = HashMap<String, String>()
    private val argList = ArrayList<String>()

    init {
        val ptnRegex = Regex("-(\\w+)")
        var lastKey: String? = null
        args.forEach { arg ->
            val key = lastKey
            when {
                arg.matches(ptnRegex) -> {
                    val tmpKey = arg.replace(ptnRegex, "$1")
                    paramMap[tmpKey] = ""
                    lastKey = tmpKey
                }
                key != null -> {
                    paramMap[key] = arg
                    lastKey = null
                }
                else -> {
                    argList.add(arg)
                    lastKey = null
                }
            }
        }
    }

    fun getParameter(key: String): String? = paramMap[key]

    fun hasParameter(key: String): Boolean = paramMap[key] != null

    fun argument(index: Int): String = argList[index]

    fun argumentCount(): Int = argList.size

    override fun toString(): String {
        return "Argument=$argList , Parameters=$paramMap"
    }
}