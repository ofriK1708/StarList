import { Send, Bot, User, Plus, RefreshCw, Sparkles } from "lucide-react";
import { useState } from "react";
import { Button } from "./ui/button";

interface Message {
  id: string;
  type: 'user' | 'ai' | 'task-suggestion';
  content: string;
  timestamp: Date;
  taskData?: {
    title: string;
    difficulty: 'easy' | 'medium' | 'hard';
    reward: number;
  };
}

interface AIChatProps {
  messages: Message[];
  onSendMessage: (message: string, newConversation?: boolean) => void;
  onAddTask: (taskData: any) => void;
  onDailyBriefing: () => void;
  isLoading?: boolean;
}

export function AIChat({ messages, onSendMessage, onAddTask, onDailyBriefing, isLoading }: AIChatProps) {
  const [input, setInput] = useState('');
  const [isNewConversation, setIsNewConversation] = useState(false);

  const handleSend = () => {
    if (input.trim() && !isLoading) {
      onSendMessage(input, isNewConversation);
      setInput('');
      setIsNewConversation(false);
    }
  };

  const handleNewChat = () => {
    setIsNewConversation(true);
    onSendMessage('', true);
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <div className="w-full h-full bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 flex flex-col">
      {/* Header */}
      <div className="bg-slate-900/95 backdrop-blur-sm border-b border-slate-700/50 px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-gradient-to-br from-purple-500 to-blue-500 flex items-center justify-center">
              <Bot className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-lg text-white/90">AI Assistant</h1>
              <p className="text-xs text-green-400">● Online</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Button
              onClick={onDailyBriefing}
              disabled={isLoading}
              variant="outline"
              size="sm"
              className="border-purple-500/50 text-purple-300 hover:bg-purple-500/20 disabled:opacity-50"
            >
              <Sparkles className="w-4 h-4 mr-1" />
              Briefing
            </Button>
            <Button
              onClick={handleNewChat}
              disabled={isLoading}
              variant="outline"
              size="sm"
              className="border-slate-600 text-slate-400 hover:bg-slate-700 disabled:opacity-50"
            >
              <RefreshCw className="w-4 h-4 mr-1" />
              New Chat
            </Button>
          </div>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {messages.length === 0 && (
          <div className="flex flex-col items-center justify-center h-full text-center gap-4 text-slate-500">
            <Bot className="w-12 h-12 opacity-30" />
            <p className="text-sm">Ask me about your tasks, habits, or let me help you add new ones!</p>
          </div>
        )}
        {messages.map((message) => (
          <div
            key={message.id}
            className={`flex items-start gap-3 ${
              message.type === 'user' ? 'flex-row-reverse' : 'flex-row'
            }`}
          >
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${
                message.type === 'user'
                  ? 'bg-blue-600'
                  : 'bg-gradient-to-br from-purple-500 to-blue-500'
              }`}
            >
              {message.type === 'user' ? (
                <User className="w-5 h-5 text-white" />
              ) : (
                <Bot className="w-5 h-5 text-white" />
              )}
            </div>

            <div
              className={`max-w-[70%] ${
                message.type === 'user' ? 'items-end' : 'items-start'
              }`}
            >
              {message.type === 'task-suggestion' && message.taskData ? (
                <div className="bg-slate-800 border border-slate-700 rounded-lg p-4 space-y-3">
                  <p className="text-sm text-slate-300 mb-3">{message.content}</p>
                  <div className="bg-slate-700/50 rounded-lg p-3 border border-slate-600">
                    <h4 className="text-white/90 mb-1">{message.taskData.title}</h4>
                    <div className="flex items-center justify-between text-sm">
                      <span
                        className={`px-2 py-1 rounded text-xs ${
                          message.taskData.difficulty === 'easy'
                            ? 'bg-green-500/20 text-green-300'
                            : message.taskData.difficulty === 'medium'
                            ? 'bg-yellow-500/20 text-yellow-300'
                            : 'bg-red-500/20 text-red-300'
                        }`}
                      >
                        {message.taskData.difficulty}
                      </span>
                      <span className="text-yellow-400">+{message.taskData.reward} coins</span>
                    </div>
                  </div>
                  <Button
                    onClick={() => onAddTask(message.taskData)}
                    className="w-full bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center gap-2"
                    size="sm"
                  >
                    <Plus className="w-4 h-4" />
                    Add to Tasks
                  </Button>
                </div>
              ) : (
                <div
                  className={`rounded-2xl px-4 py-2 ${
                    message.type === 'user'
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-800 text-slate-200 border border-slate-700'
                  }`}
                >
                  <p className="text-sm whitespace-pre-wrap">{message.content}</p>
                </div>
              )}
              <span className="text-xs text-slate-500 mt-1 block px-2">
                {message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </span>
            </div>
          </div>
        ))}

        {/* Typing indicator */}
        {isLoading && (
          <div className="flex items-start gap-3">
            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-blue-500 flex items-center justify-center shrink-0">
              <Bot className="w-5 h-5 text-white" />
            </div>
            <div className="bg-slate-800 border border-slate-700 rounded-2xl px-4 py-3">
              <div className="flex gap-1 items-center">
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                <span className="w-2 h-2 bg-slate-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Input */}
      <div className="bg-slate-900/95 backdrop-blur-sm border-t border-slate-700/50 px-6 py-4">
        <div className="flex items-end gap-3">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyPress}
            disabled={isLoading}
            placeholder="Ask about your tasks, add a habit, or get productivity tips..."
            className="flex-1 bg-slate-800 border border-slate-700 rounded-lg px-4 py-3 text-white placeholder-slate-500 resize-none focus:outline-none focus:border-blue-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            rows={1}
            style={{ minHeight: '44px', maxHeight: '120px' }}
          />
          <Button
            onClick={handleSend}
            disabled={!input.trim() || isLoading}
            className="bg-blue-600 hover:bg-blue-700 text-white disabled:opacity-50 disabled:cursor-not-allowed h-11 px-4"
          >
            <Send className="w-5 h-5" />
          </Button>
        </div>
      </div>
    </div>
  );
}
