// IMessegeService.aidl
package io.metamask;
import io.metamask.IMessegeServiceCallback;

// Declare any non-default types here with import statements

interface IMessegeService {
    void registerCallback(in IMessegeServiceCallback callback);
    void sendMessage(inout @nullable Bundle message);
}