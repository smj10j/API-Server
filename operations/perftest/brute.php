<?php

$server = 'SERVER_HERE';
$port = '80';
$path = "";
$url = "http://$server:$port/$path";

$fps = array();


while(true) {

        $fps[] = curl_post_async($url);

//      usleep(10);

/*      foreach($fps as $fp) {
                pingSocket($fp);
        }
*/
}

function curl_post_async($url,$post_string='') {

    $parts=parse_url($url);

    $timeout = 600;

    $fp = fsockopen($parts['host'],
        isset($parts['port'])?$parts['port']:80,
        $errno, $errstr, $timeout);

    if($fp==0) {
        echo "Couldn't open a socket to ".$url." (".$errstr.")";
        return false;
    }


    $out = "GET ".$parts['path']." HTTP/1.1\r\n";
    $out.= "Host: ".$parts['host']."\r\n";
    $out.= "Content-Type: application/x-www-form-urlencoded\r\n";
    $out.= "Content-Length: ".strlen($post_string)."\r\n";
    $out.= "Keep-Alive: $timeout\r\n";
    $out.= "timeout: $timeout\r\n";
    $out.= "Connection: keep-alive\r\n\r\n";
    if (isset($post_string)) $out.= $post_string;

    fwrite($fp, $out);


    stream_set_blocking($fp,FALSE);
    stream_set_timeout($fp,$timeout);

    return $fp;
}
