package jp.takoa.mojibattle.registry;

/** Registry種別をIDと要素型の組で表す型付きキー。 */
public record RegistryKey<T>(RegistryId id, Class<T> elementType) {
    public RegistryKey {
        if (id == null || elementType == null) {
            throw new IllegalArgumentException("Registry key fields are required");
        }
    }

    public static <T> RegistryKey<T> create(String id, Class<T> elementType) {
        return new RegistryKey<>(RegistryId.parse(id), elementType);
    }
}
