package android.util

object Log {
    @JvmStatic fun d(tag: String?, msg: String?): Int {
        println("DEBUG: [$tag] $msg")
        return 0
    }

    @JvmStatic fun e(tag: String?, msg: String?): Int {
        System.err.println("ERROR: [$tag] $msg")
        return 0
    }

    // Add other methods if needed...
}
