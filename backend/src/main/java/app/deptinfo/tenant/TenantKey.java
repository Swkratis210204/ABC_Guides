package app.deptinfo.tenant;

/** Identifies one tenant: a university slug plus a department slug (both lowercase, URL-safe). */
public record TenantKey(String uni, String dept) {

    @Override
    public String toString() {
        return uni + "/" + dept;
    }
}
