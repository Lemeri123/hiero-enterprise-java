package com.openelements.hiero.base.test;

import com.hedera.hashgraph.sdk.FileId;
import com.openelements.hiero.base.HieroException;
import com.openelements.hiero.base.implementation.FileClientImpl;
import com.openelements.hiero.base.protocol.FileCreateResult;
import com.openelements.hiero.base.protocol.FileCreateRequest;
import com.openelements.hiero.base.protocol.FileAppendRequest;
import com.openelements.hiero.base.protocol.FileAppendResult;
import com.openelements.hiero.base.protocol.FileInfoRequest;
import com.openelements.hiero.base.protocol.FileInfoResponse;
import com.openelements.hiero.base.protocol.ProtocolLayerClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class FileClientImplTest {
    ProtocolLayerClient protocolLayerClient;
    FileClientImpl fileClientImpl;

    @BeforeEach
    void setup() {
        protocolLayerClient = Mockito.mock(ProtocolLayerClient.class);
        fileClientImpl = new FileClientImpl(protocolLayerClient);
    }

    @Test
    void testCreateFile() throws HieroException {
        // mock
        final FileId fileId = FileId.fromString("1.2.3");
        final FileCreateResult fileCreateResult = Mockito.mock(FileCreateResult.class);

        // given
        final byte[] content = "Hello Hiero!".getBytes();

        //then
        when(protocolLayerClient.executeFileCreateTransaction(any(FileCreateRequest.class)))
                .thenReturn(fileCreateResult);
        when(fileCreateResult.fileId()).thenReturn(fileId);

        final FileId result = fileClientImpl.createFile(content);

        verify(protocolLayerClient, times(1))
                .executeFileCreateTransaction(any(FileCreateRequest.class));
        verify(fileCreateResult, times(1)).fileId();
        Assertions.assertEquals(fileId, result);
    }

    @Test
    void testCreateFileForSizeGreaterThanFileCreateMaxSize() throws HieroException {
        // mock
        final FileId fileId = FileId.fromString("1.2.3");
        final FileCreateResult fileCreateResult = Mockito.mock(FileCreateResult.class);
        final FileAppendResult fileAppendResult = Mockito.mock(FileAppendResult.class);

        // given
        final byte[] content = new byte[FileCreateRequest.FILE_CREATE_MAX_SIZE * 2];
        // -1 because 1 for executeFileCreateTransaction()
        final int appendCount = Math.floorDiv(content.length, FileCreateRequest.FILE_CREATE_MAX_SIZE) - 1;

        //then
        when(protocolLayerClient.executeFileCreateTransaction(any(FileCreateRequest.class)))
                .thenReturn(fileCreateResult);
        when(fileCreateResult.fileId()).thenReturn(fileId);
        when(protocolLayerClient.executeFileAppendRequestTransaction(any(FileAppendRequest.class)))
                .thenReturn(fileAppendResult);

        final FileId result = fileClientImpl.createFile(content);

        verify(protocolLayerClient, times(1))
                .executeFileCreateTransaction(any(FileCreateRequest.class));
        verify(fileCreateResult, times(1)).fileId();
        verify(protocolLayerClient, times(appendCount))
                .executeFileAppendRequestTransaction(any(FileAppendRequest.class));
        Assertions.assertEquals(fileId, result);
    }

    @Test
    void testCreateFileThrowsExceptionForSizeGreaterThanMaxFileSize() {
        final String message = "File contents must be less than " + FileCreateRequest.FILE_MAX_SIZE + " bytes";
        // given
        final byte[] contents = new byte[FileCreateRequest.FILE_MAX_SIZE + 1];

        // then
        final HieroException exception = Assertions.assertThrows(
                HieroException.class, () -> fileClientImpl.createFile(contents)
        );
        Assertions.assertTrue(exception.getMessage().contains(message));
    }

    @Test
    void testCreateFileThrowsExceptionForExpirationTimeBeforeNow() {
        final String message = "Expiration time must be in the future";
        // given
        final byte[] contents = "Hello Hiero!".getBytes();
        final Instant expiration = Instant.now().minusSeconds(1);

        // then
        final IllegalArgumentException exception =Assertions.assertThrows(
                IllegalArgumentException.class, () -> fileClientImpl.createFile(contents, expiration)
        );
        Assertions.assertTrue(exception.getMessage().contains(message));
    }

    @Test
    void testCreateFileThrowsExceptionForNullContent() {
        final String message = "contents must not be null";

        final NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class, () -> fileClientImpl.createFile(null)
        );
        Assertions.assertTrue(exception.getMessage().contains(message));
    }

    @Test
    void testGetFileSize() throws HieroException {
        // mocks
        final int size = 10;
        final FileInfoResponse response = Mockito.mock(FileInfoResponse.class);

        // given
        final FileId fileId = FileId.fromString("1.2.3");

        // then
        when(response.size()).thenReturn(size);
        when(protocolLayerClient.executeFileInfoQuery(any(FileInfoRequest.class)))
                .thenReturn(response);

        final int result = fileClientImpl.getSize(fileId);

        verify(protocolLayerClient, times(1))
                .executeFileInfoQuery(any(FileInfoRequest.class));
        verify(response, times(1)).size();
        Assertions.assertEquals(size, result);
    }

    @Test
    void testGetFileSizeThrowsExceptionForInvalidId() throws HieroException {
        // given
        final FileId fileId = FileId.fromString("1.2.3");

        // then
        when(protocolLayerClient.executeFileInfoQuery(any(FileInfoRequest.class)))
                .thenThrow(new HieroException("Failed to execute query"));

        Assertions.assertThrows(HieroException.class, () -> fileClientImpl.getSize(fileId));
    }

    @Test
    void testGetFileSizeThrowsExceptionForNullId() {
        final String message = "fileId must not be null";

        final NullPointerException exception = Assertions.assertThrows(
                NullPointerException.class, () -> fileClientImpl.getSize(null)
        );
        Assertions.assertTrue(exception.getMessage().contains(message));
    }
}
