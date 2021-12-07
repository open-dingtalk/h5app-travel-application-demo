/**
 *  Created by pw on 2021/11/24 下午5:54.
 */
import React, { useContext } from 'react'
import { Button } from 'antd-mobile'
import { useNavigate } from 'react-router-dom'
import './index.css'
import { UserInfoContext } from '../../App'

const Main = () => {
  let navigate = useNavigate()
  const context = useContext(UserInfoContext)

  const goTo = (path) => {
    console.log('---path----', path)
    navigate(path)
  }
  return (
    <div className="main">
      <div className={'title'}>操作列表</div>
      <Button
        className={'button'}
        block
        color="primary"
        onClick={() => {
          goTo('/apply')
        }}
      >
        差旅申请
      </Button>
      <Button
        className={'button'}
        block
        color="primary"
        onClick={() => {
          goTo('/todoList')
        }}
      >
        审批待办
      </Button>
    </div>
  )
}
export default Main
