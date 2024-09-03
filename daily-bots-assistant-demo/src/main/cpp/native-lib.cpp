#include <jni.h>
#include <malloc.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>

#include <iostream>
#include <sstream>

#include "lua/lua.hpp"

#define DAILY_DISALLOW_COPY(x) \
    x(x const&) = delete;      \
    void operator=(x const& x) = delete;

class UnownedJavaString {
    DAILY_DISALLOW_COPY(UnownedJavaString)

public:
    UnownedJavaString(JNIEnv *const env, const jstring javaString)
            : mEnv(env),
              mJavaString(javaString),
              mCString(
                      javaString == nullptr
                      ? nullptr
                      : env->GetStringUTFChars(javaString, nullptr)
              ) {}

    const char *cStrOrNull() const { return mCString; }

    ~UnownedJavaString() {
        if (mCString != nullptr) {
            mEnv->ReleaseStringUTFChars(mJavaString, mCString);
        }
    }

private:
    JNIEnv *const mEnv;
    const jstring mJavaString;
    const char *const mCString;
};

void throwRuntimeException(JNIEnv *const env, const char *const message) {

    __android_log_print(
            ANDROID_LOG_ERROR,
            "NativeLib",
            "Throwing Java exception: %s",
            message);

    const jclass runtimeExceptionClass = env->FindClass("java/lang/RuntimeException");

    if (runtimeExceptionClass != nullptr) {
        env->ThrowNew(runtimeExceptionClass, message);
        env->DeleteLocalRef(runtimeExceptionClass);
    } else {
        abort();
    }
}

int my_print(lua_State *L) {
    // Retrieve the stringstream from the upvalue
    std::ostringstream *ss = (std::ostringstream*)lua_touserdata(L, lua_upvalueindex(1));

    int nargs = lua_gettop(L);
    for (int i = 1; i <= nargs; ++i) {
        if (lua_isstring(L, i)) {
            *ss << lua_tostring(L, i);
        } else {
            // Print the type if it's not a string
            *ss << luaL_typename(L, i) << ':' << lua_topointer(L, i);
        }
        if (i < nargs) *ss << " ";
    }
    *ss << std::endl;
    return 0;
}

class LuaState {
public:
    LuaState() : state(luaL_newstate()) {
        // TODO no IO/etc access?
        luaL_openlibs(state);

        // Push stringstream pointer as a light userdata
        lua_pushlightuserdata(state, &output);

        // Push my_print as a C function closure with one upvalue (the stringstream pointer)
        lua_pushcclosure(state, my_print, 1);
        lua_setglobal(state, "print");
    }

    int run(const char *code) {
        return luaL_dostring(state, code);
    }

    std::string getError() {
        return lua_tostring(state, -1);
    }

    std::string takeOutput() {
        return output.str();
    }

    ~LuaState() {
        lua_close(state);
    }

private:
    lua_State *state;
    std::ostringstream output;
};

extern "C"
JNIEXPORT jstring JNICALL Java_co_daily_bots_assistant_tools_ToolProviderRunLua_nativeRunCode(
        JNIEnv *const env,
        const jobject thiz,
        const jstring code
) {
    UnownedJavaString codeStr(env, code);

    LuaState lua;

    const int status = lua.run(codeStr.cStrOrNull());

    if (status != LUA_OK) {
        return env->NewStringUTF(lua.getError().c_str());
    }

    return env->NewStringUTF(lua.takeOutput().c_str());
}
