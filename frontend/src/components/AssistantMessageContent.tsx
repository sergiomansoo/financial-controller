import { Fragment } from 'react'

interface Props { content: string }

function inlineContent(line: string) {
  return line.split(/(\*\*[^*\n]+\*\*)/g).map((part, index) => (
    part.startsWith('**') && part.endsWith('**')
      ? <strong key={index}>{part.slice(2, -2)}</strong>
      : <Fragment key={index}>{part}</Fragment>
  ))
}

export function AssistantMessageContent({ content }: Props) {
  return <p>{content.split('\n').map((line, index) => <Fragment key={index}>{index > 0 && <br />}{inlineContent(line)}</Fragment>)}</p>
}
