#pragma once

#ifndef __LOG_H__
#define __LOG_H__

#include <sstream>
#include <string>
#include <stdio.h>

inline std::wstring NowTime();

//enum TLogLevel {logERROR, logWARNING, logINFO, logDEBUG, logDEBUG1, logDEBUG2, logDEBUG3, logDEBUG4};
enum TLogLevel { lerror, lwarning, linfo, ldebug, ldebug1, ldebug2, ldebug3, ldebug4 };

template <typename T>
class Log
{
public:
    Log();
    virtual ~Log();
    std::wostringstream& Get(TLogLevel level = linfo);
public:
    static TLogLevel& ReportingLevel();
    static std::wstring ToString(TLogLevel level);
    static TLogLevel FromString(const std::wstring& level);
protected:
    std::wostringstream os;
private:
    Log(const Log&);
    Log& operator =(const Log&);
};

template <typename T>
Log<T>::Log()
{
}

template <typename T>
std::wostringstream& Log<T>::Get(TLogLevel level)
{
    os << "- " << NowTime();
    os << " " << ToString(level) << ": ";
    os << std::wstring(level > ldebug ? level - ldebug : 0, '\t');
    return os;
}

template <typename T>
Log<T>::~Log()
{
    os << std::endl;
    T::Output(os.str());
}

template <typename T>
TLogLevel& Log<T>::ReportingLevel()
{
    static TLogLevel reportingLevel = ldebug4;
    return reportingLevel;
}

template <typename T>
std::wstring Log<T>::ToString(TLogLevel level)
{
    static const wchar_t* const buffer[] = { L"ERROR", L"WARNING", L"INFO", L"DEBUG", L"DEBUG1", L"DEBUG2", L"DEBUG3", L"DEBUG4" };
    return buffer[level];
}

template <typename T>
TLogLevel Log<T>::FromString(const std::wstring& level)
{
    if (level == "DEBUG4")
        return ldebug4;
    if (level == L"DEBUG3")
        return ldebug3;
    if (level == L"DEBUG2")
        return ldebug2;
    if (level == L"DEBUG1")
        return ldebug1;
    if (level == L"DEBUG")
        return ldebug;
    if (level == L"INFO")
        return linfo;
    if (level == L"WARNING")
        return lwarning;
    if (level == L"ERROR")
        return lerror;
    Log<T>().Get(lwarning) << L"Unknown logging level '" << level << L"'. Using INFO level as default.";
    return linfo;
}

class Output2FILE
{
public:
    static FILE*& Stream();
    static void Output(const std::wstring& msg);
};

inline FILE*& Output2FILE::Stream()
{
    static FILE* pStream = stderr;
    return pStream;
}

inline void Output2FILE::Output(const std::wstring& msg)
{
    FILE* pStream = Stream();
    if (!pStream)
        return;
	fwprintf(pStream, L"%s", msg.c_str());
   int err = fflush(pStream);

}

#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__)
#   if defined (BUILDING_FILELOG_DLL)
#       define FILELOG_DECLSPEC   __declspec (dllexport)
#   elif defined (USING_FILELOG_DLL)
#       define FILELOG_DECLSPEC   __declspec (dllimport)
#   else
#       define FILELOG_DECLSPEC
#   endif // BUILDING_DBSIMPLE_DLL
#else
#   define FILELOG_DECLSPEC
#endif // _WIN32

class FILELOG_DECLSPEC FILELog : public Log<Output2FILE> {};
//typedef Log<Output2FILE> FILELog;

#ifndef FILELOG_MAX_LEVEL
#define FILELOG_MAX_LEVEL ldebug4
#endif

#define FILE_LOG(level) \
    if (level > FILELOG_MAX_LEVEL) ;\
    else if (level > FILELog::ReportingLevel() || !Output2FILE::Stream()) ; \
    else FILELog().Get(level)

#define L_(level) \
if (level > FILELOG_MAX_LEVEL) ;\
else if (level > FILELog::ReportingLevel() || !Output2FILE::Stream()) ; \
else FILELog().Get(level)

#if defined(WIN32) || defined(_WIN32) || defined(__WIN32__)

#include <windows.h>

inline std::wstring NowTime()
{
    const int MAX_LEN = 200;
    wchar_t buffer[MAX_LEN];
    if (GetTimeFormatW(LOCALE_USER_DEFAULT, 0, 0,
       L"HH':'mm':'ss", buffer, MAX_LEN) == 0)
        return L"Error in NowTime()";

	wchar_t result[100] = { 0 };
    static DWORD first = GetTickCount();
    swprintf_s(result, L"%s.%03ld", buffer, (long)(GetTickCount() - first) % 1000);
    return result;
}



#else

#include <sys/time.h>

inline std::string NowTime()
{
    char buffer[11];
    time_t t;
    time(&t);
    tm r = { 0 };
    strftime(buffer, sizeof(buffer), "%X", localtime_r(&t, &r));
    struct timeval tv;
    gettimeofday(&tv, 0);
    char result[100] = { 0 };
    std::sprintf(result, "%s.%03ld", buffer, (long)tv.tv_usec / 1000);
    return result;
}

#endif //WIN32

inline void initLogger(const wchar_t * file, TLogLevel level)
{
    FILELog::ReportingLevel() = level;
    FILE *log_fd;
    errno_t err = _wfopen_s(&log_fd, file, L"at,ccs=UTF-8");
    if (err != 0)
    {
        printf("Failed to open log file"  );
    }

    Output2FILE::Stream() = log_fd;
}

inline void endLogger()
{
    if (Output2FILE::Stream())
        fclose(Output2FILE::Stream());
}

#endif //__LOG_H__