# The liblcp SDK (org.readium.lcp.sdk.Lcp, DRMContext, DRMException, DRMError...)
# is accessed only through reflection (see LcpClient), so R8/Proguard cannot detect
# its usage and would otherwise strip or rename it in minified integrator apps.
# See https://github.com/readium/kotlin-toolkit/issues/204
-keep class org.readium.lcp.sdk.** { *; }
