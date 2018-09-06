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
#include <simple-obfs/libcork/include/libcork/core.h>

#define CT_HTONS(n) CORK_UINT16_HOST_TO_BIG(n)
#define CT_NTOHS(n) CORK_UINT16_BIG_TO_HOST(n)
#define CT_HTONL(n) CORK_UINT32_HOST_TO_BIG(n)
#define CT_NTOHL(n) CORK_UINT32_BIG_TO_HOST(n)

#include "base64.h"
#include "utils.h"
#include "obfs_socket5.h"

struct BSocksClient_auth_info socks_auth_info[2];
size_t socks_num_auth_info;
size_t auth_finished;

static int obfs_socket5_request(buffer_t *, size_t, obfs_t *);
static int obfs_socket5_response(buffer_t *, size_t, obfs_t *);
static int deobfs_socket5_request(buffer_t *, size_t, obfs_t *);
static int deobfs_socket5_response(buffer_t *buf, size_t cap, obfs_t *obfs);
static int obfs_app_data(buffer_t *, size_t, obfs_t *);
static int deobfs_app_data(buffer_t *, size_t, obfs_t *);
static int check_socket5_request(buffer_t *buf);
static void add_socks_auth_info();
struct BSocksClient_auth_info BSocksClient_auth_none (void);
static void disable_socket5(obfs_t *obfs);
static int is_enable_socket5(obfs_t *obfs);


static obfs_para_t obfs_socket5_st = {
    .name            = "socket5",
    .port            = 1080,
    .send_empty_response_upon_connection = true,

    .obfs_request    = &obfs_socket5_request,
    .obfs_response   = &obfs_socket5_response,
    .deobfs_request  = &deobfs_socket5_request,
    .deobfs_response = &deobfs_socket5_response,
    .check_obfs      = &check_socket5_request,
    .disable         = &disable_socket5,
    .is_enable       = &is_enable_socket5
};

obfs_para_t *obfs_socket5 = &obfs_socket5_st;

static int
obfs_app_data(buffer_t *buf, size_t cap, obfs_t *obfs)
{
//    size_t buf_len = buf->len;
//
//    brealloc(buf, buf_len + 5, cap);
//    memmove(buf->data + 5, buf->data, buf_len);
//    memcpy(buf->data, tls_data_header, 3);
//
//    *(uint16_t*)(buf->data + 3) = CT_HTONS(buf_len);
//    buf->len = buf_len + 5;

    return 0;
}

static int
deobfs_app_data(buffer_t *buf, size_t idx, obfs_t *obfs)
{
//    int bidx = idx, bofst = idx;
//
//    frame_t *frame = (frame_t *)obfs->extra;
//
//    while (bidx < buf->len) {
//        if (frame->len == 0) {
//            if (frame->idx >= 0 && frame->idx < 3
//                && buf->data[bidx] != tls_data_header[frame->idx]) {
//                return OBFS_ERROR;
//            } else if (frame->idx >= 3 && frame->idx < 5) {
//                memcpy(frame->buf + frame->idx - 3, buf->data + bidx, 1);
//            } else if (frame->idx < 0) {
//                bofst++;
//            }
//            frame->idx++;
//            bidx++;
//            if (frame->idx == 5) {
//                frame->len = CT_NTOHS(*(uint16_t *)(frame->buf));
//                frame->idx = 0;
//            }
//            continue;
//        }
//
//        if (frame->len > 16384)
//            return OBFS_ERROR;
//
//        int left_len = buf->len - bidx;
//
//        if (left_len > frame->len) {
//            memmove(buf->data + bofst, buf->data + bidx, frame->len);
//            bidx  += frame->len;
//            bofst += frame->len;
//            frame->len = 0;
//        } else {
//            memmove(buf->data + bofst, buf->data + bidx, left_len);
//            bidx  = buf->len;
//            bofst += left_len;
//            frame->len -= left_len;
//        }
//    }
//
//    buf->len = bofst;

    return OBFS_OK;
}

static int
obfs_socket5_request(buffer_t *buf, size_t cap, obfs_t *obfs)
{
    if (obfs == NULL || obfs->obfs_stage < 0) return 0;
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
        LOGI("obfs_socket5_request:%u", buf->data);
        buf->len = tls_len;

        obfs->obfs_stage++;
    }
    else if (obfs->obfs_stage == 1) {

    }
    return buf->len;
}

static int
obfs_socket5_response(buffer_t *buf, size_t cap, obfs_t *obfs)
{
    LOGI("obfs_socket5_response:%u", buf->data);
    if (obfs == NULL || obfs->obfs_stage < 0) return 0;
    if (obfs->obfs_stage == 0) {
        struct socks_server_hello imsg;
        memcpy(&imsg, buf->data, sizeof(imsg));
        if (ntoh8(imsg.ver) != SOCKS_VERSION) {
            LOGE("wrong version");
            return OBFS_ERROR;
        }
        struct socks_request_header header;

        obfs->obfs_stage++;
    }
    else if (obfs->obfs_stage == 1) {

    }
    return buf->len;
}

static int
deobfs_socket5_request(buffer_t *buf, size_t cap, obfs_t *obfs)
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
deobfs_socket5_response(buffer_t *buf, size_t cap, obfs_t *obfs)
{
    if (obfs == NULL || obfs->deobfs_stage < 0) return 0;

    if (obfs->deobfs_stage == 0) {

        struct socks_server_hello imsg;
        memcpy(&imsg, buf->data, sizeof(imsg));
        if (ntoh8(imsg.ver) != SOCKS_VERSION) {
            LOGE("wrong version");
            return OBFS_ERROR;
        }
        struct socks_request_header header;
        LOGI("deobfs_socket5_response:%u,%g", imsg.ver, imsg.method);
        obfs->deobfs_stage++;

    } else if (obfs->deobfs_stage == 1) {

        return deobfs_app_data(buf, 0, obfs);

    }

    return 0;
}

static int
check_socket5_request(buffer_t *buf)
{
    char *data = buf->data;
    int len    = buf->len;

    if (len < 11)
        return OBFS_NEED_MORE;

    if (data[0] == 0x16
        && data[1] == 0x03
        && data[2] == 0x01
        && data[5] == 0x01
        && data[9] == 0x03
        && data[10] == 0x03)
        return OBFS_OK;
    else
        return OBFS_ERROR;
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


static void
disable_socket5(obfs_t *obfs)
{
    obfs->obfs_stage = -1;
    obfs->deobfs_stage = -1;
}

static int
is_enable_socket5(obfs_t *obfs)
{
    return obfs->obfs_stage != -1 && obfs->deobfs_stage != -1;
}
