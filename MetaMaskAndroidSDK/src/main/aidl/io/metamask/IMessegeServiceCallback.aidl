// IMessegeServiceCallback.aidl
package io.metamask;

// Declare any non-default types here with import statements

interface IMessegeServiceCallback {
    void onMessageReceived(inout Bundle response);
}