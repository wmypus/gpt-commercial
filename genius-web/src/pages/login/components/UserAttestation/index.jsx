/*
 * @Description:
 * @Version: 2.0
 * @Autor: jinglin.gao
 * @Date: 2023-04-08 10:06:25
 * @LastEditors: jinglin.gao
 * @LastEditTime: 2023-04-13 11:27:30
 */
import React, { useState, forwardRef, useImperativeHandle } from "react";
import { Input, Button } from "antd";
import _ from "lodash";
import styles from "./index.module.less";
import { CloseOutlined } from "@ant-design/icons";
import { imageVerificationCode } from "@/api/login";
const UserAttestation = forwardRef(
  ({ phoneNumber, setImgVerificationCode }, ref) => {
    const [pageState, setPageState] = useState(false);
    const [verificationCode, setVerificationCode] = useState("");

    const [vcerificationCodeImg, setVcerificationCodeImg] = useState("");
    useImperativeHandle(ref, () => {
      return {
        getPage,
      };
    });

    /**
     * @description: 弹框展示
     * @return {*}
     * @author: jinglin.gao
     */
    const getPage = () => {
      setPageState(true);
      getImageVerificationCode();
    };

    /**
     * @description: 弹框隐藏
     * @return {*}
     * @author: jinglin.gao
     */

    const hidePage = () => {
      setPageState(false);
    };

    /**
     * @description: 获取验证码
     * @return {*}
     * @author: jinglin.gao
     */
    const getImageVerificationCode = async () => {
      try {
        let params = {
          phone: phoneNumber,
        };
        let res = await imageVerificationCode(params);
        if (res) {
          let resImg = window.URL.createObjectURL(res);
          setVcerificationCodeImg(resImg);
        }
      } catch (error) {
        console.log(error);
      }
    };

    const changeVcerificationCode = () => {
      getImageVerificationCode();
    };

    const submit = () => {
      setImgVerificationCode(verificationCode);
      hidePage();
    };

    return (
      <>
        {pageState ? (
          <div className={styles.custom_dialog}>
            <div className="custom_dialog-warp">
              <div className="custom_dialog-head">
                <span className="title">访问验证</span>
                <CloseOutlined
                  onClick={hidePage}
                  className="closeBtn"
                  twoToneColor="#fff"
                ></CloseOutlined>
              </div>

              <div className="custom_dialog-content">
                <p className="title">
                  亲，这是个机器猖狂的时代，请输入验证码证明咱是正常人！
                </p>

                <Input
                  maxLength={11}
                  value={verificationCode}
                  onChange={(e) => setVerificationCode(e.target.value)}
                  placeholder="请输入下图中的字符,不区分大小写"
                />
                <div className="vcerification_code-img-box">
                  {vcerificationCodeImg ? (
                    <img
                      className="vcerification_code-img"
                      src={vcerificationCodeImg}
                      alt=""
                    />
                  ) : (
                    ""
                  )}

                  <p
                    onClick={changeVcerificationCode}
                    className="vcerification_code-info"
                  >
                    看不清楚？换一个
                  </p>

                  <div className="user_detail_bottom">
                    <Button
                      onClick={_.debounce(submit, 500)}
                      className="tolls_btn"
                    >
                      提交
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : (
          ""
        )}
      </>
    );
  }
);

export default UserAttestation;
