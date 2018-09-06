package main

import (
	"strings"
	"path/filepath"
	"os"
	"log"
	"net/http"
	"io/ioutil"
	"encoding/json"
	"path"
	"sync"
)
const (
	snssdk  = "https://lf.snssdk.com/api/news/feed/v88/?list_count=0&support_rn=0&category=hotsoon_video&refer=1&count=20&min_behot_time=1534390143&list_entrance=main_tab&last_refresh_sub_entrance_interval=1534390739&loc_mode=7&tt_from=pull&lac=33289&cid=136467975&plugin_enable=3&iid=40951047282&device_id=51444803304&ac=wifi&channel=huawei&aid=13&app_name=news_article&version_code=685&version_name=6.8.5&device_platform=android&ab_version=261581%2C403270%2C457480%2C397707%2C405108%2C458327%2C420726%2C455025%2C271178%2C424179%2C357705%2C377637%2C326524%2C326532%2C452011%2C457813%2C415914%2C409844%2C449897%2C445650%2C239098%2C448295%2C170988%2C452076%2C451966%2C374118%2C437001%2C434626%2C454399%2C443147%2C451675%2C276206%2C439194%2C453555%2C435215%2C431682%2C446378%2C277718%2C435977%2C440667%2C381406%2C416055%2C448243%2C392461%2C448802%2C433402%2C456439%2C444653%2C429620%2C378450%2C457329%2C449384%2C437652%2C442789%2C425531%2C323233%2C453207%2C443359%2C423342%2C435309%2C345191%2C455945%2C458306%2C424606%2C454748%2C451977%2C455646%2C449327%2C456653%2C423997%2C424176%2C436494%2C214069%2C31207%2C439145%2C442255%2C399091%2C455390%2C453418%2C443501%2C449395%2C280447%2C281299%2C325617%2C447963%2C357401%2C420789%2C386891%2C427045%2C397990%2C451414%2C426318%2C457395%2C416639%2C441403%2C444464%2C444499%2C456733&ab_client=a1%2Cc4%2Ce1%2Cf1%2Cg2%2Cf7&ab_feature=94563%2C102749&abflag=3&ssmix=a&device_type=EML-AL00&device_brand=HUAWEI&language=zh&os_api=27&os_version=8.1.0&uuid=869839034950783&openudid=1d4a25b01c9d73ee&manifest_version_code=685&resolution=1080*2244&dpi=480&update_version_code=68509&_rticket=1534390739760&plugin=26991&pos=5r_88Pzt3vTp5L-nv3sVDXQeIHglH7-xv_zw_O3R8vP69Ono-fi_p6ytqbOtq6qorKWxv_zw_O3R_On06ej5-L-nrq2zqKitrKqp4A%3D%3D&fp=RrTqPYcSLrLZFlcSLlU1FYwIJ2K7&rom_version=emotionui_8.1.0_eml-al00+8.1.0.132%28c00%29&ts=1534390741&as=a245afd7153d2b41442685&mas=00e44c327804e19edd3ec4b3e0f292298446ece686a0d265a0&cp=53b9714afc1d3q1"
	resume = 0x2
	stop   = 0x4
)

type ShortVideo struct{
	title string
	pic_url string
	video_url string
}

var wg sync.WaitGroup
func main()  {
	dir_path := getCurrentDirectory()
	log.Println(dir_path)
	client := &http.Client{}
	for _, v := range getVideoList() {
		wg.Add(1)
		resp, err := client.Get(v.pic_url)
		if err != nil {
			// handle error
		}
		defer resp.Body.Close()
		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			// handle error
		}
		//log.Println(v.video_url)
		filename := filepath.Base(v.pic_url)
		fileSuffix := path.Ext(filename)
		go download_video(v.video_url, strings.TrimSuffix(filename, fileSuffix))
		os.MkdirAll(dir_path + "/pic/test/", os.ModeAppend)
		ioutil.WriteFile(dir_path + "/pic/test/" +  filename, body, os.ModeAppend)
	}
	wg.Wait()
	//download_video()
}

func download_video(url string, name string)  {
	dir_path := getCurrentDirectory()
	client := &http.Client{}
	req, err := http.NewRequest("GET", url, strings.NewReader("1=1"))
	req.Header.Set("User-Agent","Dalvik/2.1.0 (Linux; U; Android 8.1.0; EML-AL00 Build/HUAWEIEML-AL00) NewsArticle/6.8.5 cronet/TTNetVersion:pre_blink_merge-277476-g1fcd8719 2018-08-02")
	req.Header.Add("Cookie","sessionid=d8c50874db0d7b3d0a44a4395ee534aa")
	resp, err := client.Do(req)
	log.Println("downloading: " + url)
	if err != nil {
		log.Println("error: " + err.Error())
		defer wg.Done()
		return
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Println("error: " + err.Error())
		defer wg.Done()
		return
	}
	log.Println("saveing: " + url)
	os.MkdirAll(dir_path + "/pic/video/", os.ModeAppend)
	ioerr:=ioutil.WriteFile(dir_path + "/pic/video/" +  name + ".mp4", body, os.ModeAppend)
	if ioerr != nil {
		log.Println("error: " + ioerr.Error())
		defer wg.Done()
		return
	}
	defer wg.Done()
}

func getVideoList() []ShortVideo {
	client := &http.Client{}
	req, err := http.NewRequest("GET", snssdk, strings.NewReader("1=1"))
	if err != nil {
		// handle error
	}
	req.Header.Set("User-Agent","Dalvik/2.1.0 (Linux; U; Android 8.1.0; EML-AL00 Build/HUAWEIEML-AL00) NewsArticle/6.8.5 cronet/TTNetVersion:pre_blink_merge-277476-g1fcd8719 2018-08-02")
	req.Header.Add("Cookie","sessionid=d8c50874db0d7b3d0a44a4395ee534aa")
	resp, err := client.Do(req)
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		// handle error
	}
	var resultJson map[string]interface{}
	json.Unmarshal(body, &resultJson)
	dataAry := resultJson["data"].([]interface{})
	//log.Println(reflect.TypeOf(dataJson["content"]))
	var shortVideo []ShortVideo
	for _, v := range dataAry {
		sVideo := new(ShortVideo)
		dataJson := v.(map[string]interface{})
		var contentJson map[string]interface{}
		json.Unmarshal([]byte(dataJson["content"].(string)), &contentJson)
		raw_data := contentJson["raw_data"].(map[string]interface{})
		//log.Println(raw_data)
		sVideo.title = raw_data["label"].(string)
		imgUrlAry := raw_data["thumb_image_list"].([]interface{})
		for _, value := range imgUrlAry {
			sVideo.pic_url = value.(map[string]interface{})["url"].(string)
			break
		}
		video:= raw_data["video"].(map[string]interface{})
		videoPlayAddr:= video["play_addr"].(map[string]interface{})
		videoUrlAry:= videoPlayAddr["url_list"].([]interface{})
		for _, value := range videoUrlAry {
			sVideo.video_url = value.(string)
			break
		}
		shortVideo = append(shortVideo, *sVideo)
	}
	return shortVideo
}

/*
获取程序运行路径
*/
func getCurrentDirectory() string {
	dir, err := filepath.Abs(filepath.Dir(os.Args[0]))
	if err != nil {
		log.Println(err)
	}
	return strings.Replace(dir, "\\", "/", -1)
}