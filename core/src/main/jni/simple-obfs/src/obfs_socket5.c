/*
 * obfs_socket5.c - Implementation of http obfuscating
 *
 * Copyright (C) 2013 - 2016, Max Lv <max.c.lv@gmail.com>
 *
 * This file is part of the simple-obfs.
 *
 * simple-obfs is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * simple-obfs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with simple-obfs; see the file COPYING. If not, see
 * <http://www.gnu.org/licenses/>.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <strings.h>
#include <ctype.h> /* isblank() */

#include "base64.h"
#include "utils.h"
#include "obfs_socket5.h"

struct BSocksClient_auth_info socks_auth_info[2];
size_t socks_num_auth_info;

static const char *http_request_template =
    "GET %s HTTP/1.1\r\n"
    "Host: %s\r\n"
    "User-Agent: curl/7.%d.%d\r\n"
    "Upgrade: websocket\r\n"
    "Connection: Upgrade\r\n"
    "Sec-WebSocket-Key: %s\r\n"
    "Content-Length: %lu\r\n"
    "\r\n";

static const char *http_response_template =
    "HTTP/1.1 101 Switching Protocols\r\n"
    "Server: nginx/1.%d.%d\r\n"
    "Date: %s\r\n"
    "Upgrade: websocket\r\n"
    "Connection: Upgrade\r\n"
    "Sec-WebSocket-Accept: %s\r\n"
    "\r\n";

static int obfs_socket5_request(buffer_t *, size_t, obfs_t *);
static int obfs_socket5_response(buffer_t *, size_t, obfs_t *);
static int deobfs_socket5_header(buffer_t *, size_t, obfs_t *);
static int check_http_header(buffer_t *buf);
static void disable_http(obfs_t *obfs);
static int is_enable_http(obfs_t *obfs);

static int get_header(const char *, const char *, int, char **);
static int next_header(const char **, int *);

static obfs_para_t obfs_socket5_st = {
    .name            = "socket5",
    .port            = 80,
    .send_empty_response_upon_connection = true,

    .obfs_request    = &obfs_socket5_request,
    .obfs_response   = &obfs_socket5_response,
    .deobfs_request  = &deobfs_socket5_header,
    .deobfs_response = &deobfs_socket5_header,
    .check_obfs      = &check_http_header,
    .disable         = &disable_http,
    .is_enable       = &is_enable_http
};

obfs_para_t *obfs_socket5 = &obfs_socket5_st;

static int
obfs_socket5_request(buffer_t *buf, size_t cap, obfs_t *obfs)
{
    if (obfs == NULL || obfs->obfs_stage != 0) return 0;

    static buffer_t tmp = { 0, 0, 0, NULL };
    if(obfs->obfs_stage == 0) {
        size_t tls_len = buf->len;
        // write hello header
        add_socks_auth_info();
        struct socks_client_hello_header header;
        header.ver = hton8(SOCKS_VERSION);
        header.nmethods = hton8(socks_num_auth_info);
        tls_len += sizeof(header);
        memcpy(buf->data, &header, sizeof(header));
        // write hello methods
        for (size_t i = 0; i < socks_num_auth_info; i++) {
            struct socks_client_hello_method method;
            method.method = hton8(socks_auth_info[i].auth_type);
            tls_len += sizeof(method);
            memcpy(buf->data + sizeof(header) + i * sizeof(method), &method, sizeof(method));
        }
        buf->len = tls_len;

        obfs->obfs_stage++;
    }
    else if (obfs->obfs_stage == 1) {

        static int major_version = 0;
        static int minor_version = 0;

        major_version = major_version ? major_version : rand() % 51;
        minor_version = minor_version ? minor_version : rand() % 2;

        char host_port[256];
        char http_header[512];
        uint8_t key[16];
        char b64[64];

        if (obfs_socket5->port != 80)
            snprintf(host_port, sizeof(host_port), "%s:%d", obfs_socket5->host, obfs_socket5->port);
        else
            snprintf(host_port, sizeof(host_port), "%s", obfs_socket5->host);

        rand_bytes(key, 16);
        base64_encode(b64, 64, key, 16);

        size_t obfs_len =
                snprintf(http_header, sizeof(http_header), http_request_template,
                         obfs_socket5->uri, host_port, major_version, minor_version, b64, buf->len);
        size_t buf_len = buf->len;

        brealloc(buf, obfs_len + buf_len, cap);

        memmove(buf->data + obfs_len, buf->data, buf_len);
        memcpy(buf->data, http_header, obfs_len);

        buf->len = obfs_len + buf_len;
    }
    return buf->len;
}

static void
add_socks_auth_info()
{
    // add none socks authentication method
    socks_auth_info[0] = BSocksClient_auth_none();
    socks_num_auth_info = 1;

    // add password socks authentication method
//    if (options.username) {
//        const char *password;
//        size_t password_len;
//        if (options.password) {
//            password = options.password;
//            password_len = strlen(options.password);
//        } else {
//            if (!read_file(options.password_file, &password_file_contents, &password_len)) {
//                BLog(BLOG_ERROR, "failed to read password file");
//                return 0;
//            }
//            password = (char *)password_file_contents;
//        }
//
//        socks_auth_info[socks_num_auth_info++] = BSocksClient_auth_password(
//            options.username, strlen(options.username),password, password_len);
//    }
}
struct BSocksClient_auth_info BSocksClient_auth_none (void)
{
    struct BSocksClient_auth_info info;
    info.auth_type = SOCKS_METHOD_NO_AUTHENTICATION_REQUIRED;
    return info;
}

static int
obfs_socket5_response(buffer_t *buf, size_t cap, obfs_t *obfs)
{

    if (obfs == NULL || obfs->obfs_stage != 0) return 0;

    if (obfs->obfs_stage == 0) {
        static buffer_t tmp = { 0, 0, 0, NULL };
        size_t buf_len = buf->len;
        size_t hello_len = sizeof(struct tls_server_hello);
        size_t tls_len = buf_len + hello_len;

        brealloc(&tmp, buf_len, cap);
        brealloc(buf,  tls_len, cap);

        memcpy(tmp.data, buf->data, buf_len);
        obfs->obfs_stage++;
    }
    else if (obfs->obfs_stage == 1) {

        static int major_version = 0;
        static int minor_version = 0;

        major_version = major_version ? major_version : rand() % 11;
        minor_version = minor_version ? minor_version : rand() % 12;

        char http_header[512];
        char datetime[64];
        uint8_t key[16];
        char b64[64];

        time_t now;
        struct tm *tm_now;

        time(&now);
        tm_now = localtime(&now);
        strftime(datetime, 64, "%a, %d %b %Y %H:%M:%S GMT", tm_now);

        rand_bytes(key, 16);
        base64_encode(b64, 64, key, 16);

        size_t buf_len = buf->len;
        size_t obfs_len =
                snprintf(http_header, sizeof(http_header), http_response_template,
                         major_version, minor_version, datetime, b64);

        brealloc(buf, obfs_len + buf_len, cap);

        memmove(buf->data + obfs_len, buf->data, buf_len);
        memcpy(buf->data, http_header, obfs_len);

        buf->len = obfs_len + buf_len;
    }
    return buf->len;
}

static int
deobfs_socket5_header(buffer_t *buf, size_t cap, obfs_t *obfs)
{
    if (obfs == NULL || obfs->deobfs_stage != 0) return 0;

    char *data = buf->data;
    int len    = buf->len;
    int err    = -1;

    // Allow empty content
    while (len >= 4) {
        if (data[0] == '\r' && data[1] == '\n'
            && data[2] == '\r' && data[3] == '\n') {
            len  -= 4;
            data += 4;
            err   = 0;
            break;
        }
        len--;
        data++;
    }

    if (!err) {
        memmove(buf->data, data, len);
        buf->len = len;
        obfs->deobfs_stage++;
    }

    return err;
}

static int
check_http_header(buffer_t *buf)
{
    char *data = buf->data;
    int len    = buf->len;

    if (len < 4)
        return OBFS_NEED_MORE;

    if (strncasecmp(data, "GET", 3) != 0)
        return OBFS_ERROR;

    {
        char *protocol;
        int result = get_header("Upgrade:", data, len, &protocol);
        if (result < 0) {
            if (result == -1)
                return OBFS_NEED_MORE;
            else
                return OBFS_ERROR;
        }
        if (strncmp(protocol, "websocket", result) != 0) {
            free(protocol);
            return OBFS_ERROR;
        } else {
            free(protocol);
        }
    }

    if (obfs_socket5->host != NULL) {
        char *hostname;
        int i;

        int result = get_header("Host:", data, len, &hostname);
        if (result < 0) {
            if (result == -1)
                return OBFS_NEED_MORE;
            else
                return OBFS_ERROR;
        }

        /*
         *  if the user specifies the port in the request, it is included here.
         *  Host: example.com:80
         *  so we trim off port portion
         */
        for (i = result - 1; i >= 0; i--)
            if ((hostname)[i] == ':') {
                (hostname)[i] = '\0';
                result         = i;
                break;
            }

        result = OBFS_ERROR;
        if (strncasecmp(hostname, obfs_socket5->host, result) == 0) {
            result = OBFS_OK;
        }
        free(hostname);
        return result;
    }

    return OBFS_OK;
}

static int
get_header(const char *header, const char *data, int data_len, char **value)
{
    int len, header_len;

    header_len = strlen(header);

    /* loop through headers stopping at first blank line */
    while ((len = next_header(&data, &data_len)) != 0)
        if (len > header_len && strncasecmp(header, data, header_len) == 0) {
            /* Eat leading whitespace */
            while (header_len < len && isblank((unsigned char)data[header_len]))
                header_len++;

            *value = malloc(len - header_len + 1);
            if (*value == NULL)
                return -4;

            strncpy(*value, data + header_len, len - header_len);
            (*value)[len - header_len] = '\0';

            return len - header_len;
        }

    /* If there is no data left after reading all the headers then we do not
     * have a complete HTTP request, there must be a blank line */
    if (data_len == 0)
        return -1;

    return -2;
}

static int
next_header(const char **data, int *len)
{
    int header_len;

    /* perhaps we can optimize this to reuse the value of header_len, rather
     * than scanning twice.
     * Walk our data stream until the end of the header */
    while (*len > 2 && (*data)[0] != '\r' && (*data)[1] != '\n') {
        (*len)--;
        (*data)++;
    }

    /* advanced past the <CR><LF> pair */
    *data += 2;
    *len  -= 2;

    /* Find the length of the next header */
    header_len = 0;
    while (*len > header_len + 1
           && (*data)[header_len] != '\r'
           && (*data)[header_len + 1] != '\n')
        header_len++;

    return header_len;
}

static void
disable_http(obfs_t *obfs)
{
    obfs->obfs_stage = -1;
    obfs->deobfs_stage = -1;
}

static int
is_enable_http(obfs_t *obfs)
{
    return obfs->obfs_stage != -1 && obfs->deobfs_stage != -1;
}
