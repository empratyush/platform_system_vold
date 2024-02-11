#define LOG_TAG "DuressKeystore"

#include <android-base/logging.h>
#include <android-base/properties.h>
#include "../Keystore.h"

int main() {

    LOG(INFO) << "service started ";

    android::vold::Keystore *keystore = new android::vold::Keystore();
    keystore->deleteAllKeys();

    const std::string duressStartProp = "sys.duress.wipe.start";
    const std::string currentDuressProp = android::base::GetProperty(duressStartProp, "");

    LOG(INFO) << "keys in keystore erased successfully";

    if (currentDuressProp.empty()) {
        LOG(WARNING) << duressStartProp << " prop is not set running in test mode, vendor duress wipe service will not start ";
        return 0;
    }

    android::base::SetProperty(duressStartProp, "vendor");
    return 0;
}
