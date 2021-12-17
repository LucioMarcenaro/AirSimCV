#include <jni.h>
#include "carclient.h"
#include <vehicles/car/api/CarRpcLibClient.hpp>

#include <opencv2/core.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>

using namespace msr::airlib;
typedef ImageCaptureBase::ImageRequest ImageRequest;
typedef ImageCaptureBase::ImageResponse ImageResponse;
typedef ImageCaptureBase::ImageType ImageType;

CarRpcLibClient * m_client;

JNIEXPORT jboolean JNICALL Java_com_pervasive_airsimcv_MainActivity_CarConnect(JNIEnv *env, jobject, jstring jstr)
{
    if (m_client)
        return false;
    const char *ipaddr = env->GetStringUTFChars(jstr, NULL);
    m_client = new CarRpcLibClient(ipaddr);
    m_client->enableApiControl(true);
    bool isEnabled = m_client->isApiControlEnabled();
    return isEnabled;
}

JNIEXPORT jboolean JNICALL Java_com_pervasive_airsimcv_MainActivity_CarDisconnect(JNIEnv *env, jobject)
{
    if (!m_client)
        return false;
    m_client->enableApiControl(false);
    bool isEnabled = m_client->isApiControlEnabled();
    delete m_client;
    m_client = NULL;
    return !isEnabled;
}

JNIEXPORT void JNICALL Java_com_pervasive_airsimcv_MainActivity_CarForward(JNIEnv *env, jobject)
{
    if (!m_client)
        return;
    CarApiBase::CarControls controls;
    controls.throttle = 0.5f;
    controls.steering = 0.0f;
    m_client->setCarControls(controls);
}

JNIEXPORT void JNICALL Java_com_pervasive_airsimcv_MainActivity_GetImage(JNIEnv *env, jobject, jlong img)
{
    if (!m_client)
        return;

    cv::Mat& matImg = *(cv::Mat*)img;

    std::vector<ImageRequest> request = {
            ImageRequest("0", ImageType::Scene, false),
    };

    const std::vector<ImageResponse>& response = m_client->simGetImages(request);
    assert(response.size() > 0);
    if(response.size() > 0) {
        matImg = imdecode(response.at(0).image_data_uint8, cv::ImreadModes::IMREAD_COLOR);
    }
}
