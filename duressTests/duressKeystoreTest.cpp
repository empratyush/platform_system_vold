#define LOG_TAG "DuressKeystore"

#include <android-base/logging.h>
#include <android-base/properties.h>

#include "../KeyStorage.h"
#include "../Keystore.h"
#include "../KeyBuffer.h"

#include <string>
#include <filesystem>

void retrieveKey(const std::string &dir, android::vold::KeyBuffer *key);

bool areAnyValidKeyPresent();

int main() {
    LOG(INFO) << "duress keystore test started";

    const std::string status = areAnyValidKeyPresent() ?
                               "able to retrieveKey keys" :
                               "failed to retrieveKey keys";

    LOG(INFO) << status;
    return 0;
}

bool areAnyValidKeyPresent() {
    const char *dirs[3] = {
            "/metadata/vold/metadata_encryption/key",
            "/data/unencrypted/key",
            "/data/misc/vold/user_keys/de/0",
    };

    for (const char *dir: dirs) {

        if (!std::filesystem::is_directory(dir)) {
            LOG(ERROR) << "dir " << dir << "does not exist skipping";
            continue;
        }

        android::vold::KeyBuffer ce_key;
        retrieveKey(dir, &ce_key);

        std::string key;
        std::copy(ce_key.begin(), ce_key.end(), std::back_inserter(key));

        const bool isValidKey = !key.empty();

        std::string status = isValidKey ? "valid" : "invalid";
        LOG(INFO) << "dir " << dir << " contains " << status << " key size " << ce_key.size();

        if (isValidKey) {
            return true;
        }

        key.clear();
    }

    LOG(INFO) << "duress keystore test ended";
    return false;
}

void retrieveKey(const std::string &dir, android::vold::KeyBuffer *key) {
    android::vold::retrieveKey(dir, android::vold::kEmptyAuthentication, key);
}
