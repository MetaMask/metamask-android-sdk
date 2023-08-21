// IMessegeService.aidl
package io.metamask.nativesdk;
import io.metamask.nativesdk.IMessegeServiceCallback;

interface IMessegeService {
    void registerCallback(in IMessegeServiceCallback callback);
    void sendMessage(inout Bundle message);
}