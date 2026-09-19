package com.rskickbox.app

/**
 * Process-lifetime UI guards. These survive Activity recreation (for example
 * rotation) without being persisted as account/session data.
 */
object RsRuntimeV108 {
    @Volatile var introShownThisProcess:Boolean=false
}
