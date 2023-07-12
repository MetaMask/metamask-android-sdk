// IMessegeService.aidl
package io.metamask.nativesdk;
import io.metamask.nativesdk.IMessegeServiceCallback;

// Declare any non-default types here with import statements

interface IMessegeService {
    void registerCallback(in IMessegeServiceCallback callback);
    void sendMessage(inout Bundle message);
}