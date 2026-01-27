package com.medibridge.appointment_service_medibridge.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * JPA Converter for UUID ↔ BINARY(16)
 * 
 * Justification for BINARY(16):
 * - 50% storage savings vs CHAR(36)
 * - Faster indexing and joins
 * - Better performance for large datasets
 * - Industry standard for UUID in MySQL
 * 
 * Usage:
 * 
 * @Convert(converter = UUIDConverter.class)
 *                    private UUID id;
 */
@Converter(autoApply = false)
public class UUIDConverter implements AttributeConverter<UUID, byte[]> {

    @Override
    public byte[] convertToDatabaseColumn(UUID uuid) {
        return convertUUIDToBytes(uuid);
    }

    public static byte[] convertUUIDToBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    @Override
    public UUID convertToEntityAttribute(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }

        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long mostSigBits = bb.getLong();
        long leastSigBits = bb.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }
}
