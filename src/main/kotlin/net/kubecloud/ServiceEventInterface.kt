package net.kubecloud

import io.kubernetes.client.openapi.models.V1Pod

interface ServiceEventInterface {

    fun podCreated(pod: V1Pod) {}
    fun podModified(pod: V1Pod) {}
    fun podDeleted(pod: V1Pod) {}

}