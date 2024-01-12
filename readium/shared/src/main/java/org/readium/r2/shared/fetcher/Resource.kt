package org.readium.r2.shared.fetcher

@Deprecated(
    "Moved to a different package.",
    ReplaceWith("org.readium.r2.shared.util.resource.Resource"),
    DeprecationLevel.ERROR
)
public class Resource {

    @Deprecated(
        "`Resource.Exception` was split into several `Error` classes. You probably need `ReadError`.",
        ReplaceWith("org.readium.r2.shared.util.data.ReadError"),
        DeprecationLevel.ERROR
    )
    public class Exception
}
