extern "C" {
    JNIEXPORT jboolean JNICALL Java_com_pervasive_airsimcv_MainActivity_CarConnect(JNIEnv *env, jobject, jstring);
    JNIEXPORT jboolean JNICALL Java_com_pervasive_airsimcv_MainActivity_CarDisconnect(JNIEnv *env, jobject);
    JNIEXPORT void JNICALL Java_com_pervasive_airsimcv_MainActivity_CarForward(JNIEnv *env, jobject);

    JNIEXPORT void JNICALL Java_com_pervasive_airsimcv_MainActivity_GetImage(JNIEnv *env, jobject, jlong img);
}